package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/bitrise-io/go-android/v2/gradle"
	"github.com/bitrise-io/go-steputils/v2/stepconf"
	"github.com/bitrise-io/go-steputils/v2/testquarantine"
	"github.com/bitrise-io/go-utils/v2/command"
	"github.com/bitrise-io/go-utils/v2/env"
	"github.com/bitrise-io/go-utils/v2/log"
	"github.com/bitrise-io/go-utils/v2/pathutil"
	"github.com/kballard/go-shellquote"

	"github.com/stripe/stripe-android/bitrise-step-gradle-test/gradleconfig"
	"github.com/stripe/stripe-android/bitrise-step-gradle-test/output"
)

type configs struct {
	ProjectLocation string `env:"project_location,dir"`
	GradlewPath     string `env:"gradlew_path"`
	GradleCommand   string `env:"gradle_command,required"`
	AllowedRetries  int    `env:"allowed_retries"`
	HTMLResultDirs  string `env:"report_path_pattern"`
	XMLResultDirs   string `env:"result_path_pattern"`

	IsDebug          bool   `env:"is_debug,opt[true,false]"`
	QuarantinedTests string `env:"quarantined_tests"`

	DeployDir     string `env:"BITRISE_DEPLOY_DIR"`
	TestResultDir string `env:"BITRISE_TEST_RESULT_DIR"`
}

func main() {
	logger := log.NewLogger()
	if err := run(logger); err != nil {
		logger.Errorf("%s", err)
		os.Exit(1)
	}
}

func run(logger log.Logger) error {
	var cfg configs
	envRepo := env.NewRepository()
	cmdFactory := command.NewFactory(envRepo)
	pathChecker := pathutil.NewPathChecker()
	inputParser := stepconf.NewInputParser(envRepo)
	exporter := output.NewExporter(envRepo, pathChecker, logger)

	if err := inputParser.Parse(&cfg); err != nil {
		return fmt.Errorf("process config: %w", err)
	}

	if cfg.HTMLResultDirs == "" {
		cfg.HTMLResultDirs = "*build/reports/tests"
	}
	if cfg.XMLResultDirs == "" {
		cfg.XMLResultDirs = "*build/test-results"
	}
	if cfg.GradlewPath == "" {
		cfg.GradlewPath = "gradlew"
	}
	if cfg.AllowedRetries < 0 {
		logger.Warnf("allowed_retries cannot be negative, using 0 instead")
		cfg.AllowedRetries = 0
	}

	stepconf.Print(cfg)
	logger.EnableDebugLog(cfg.IsDebug)

	gradleProject, err := gradle.NewProject(cfg.ProjectLocation, cmdFactory, logger)
	if err != nil {
		return fmt.Errorf("open gradle project: %w", err)
	}

	gradleArgs, err := shellquote.Split(cfg.GradleCommand)
	if err != nil {
		return fmt.Errorf("parse gradle_command: %w", err)
	}

	testIdentifiers, err := parseQuarantinedTests(cfg.QuarantinedTests)
	if err != nil {
		return fmt.Errorf("parse quarantined_tests: %w", err)
	}

	var initScriptPath string
	if len(testIdentifiers) > 0 {
		if conflictsWithQuarantine(gradleArgs) {
			return fmt.Errorf("run: --init-script / -I cannot be used together with quarantined_tests input")
		}

		logger.Println()
		logger.Infof("%d quarantined test(s) found", len(testIdentifiers))
		logger.Printf("Writing Gradle init script for excluding quarantined tests...")
		initScriptPath, err = gradleconfig.WriteSkipTestingInitScript(testIdentifiers)
		if err != nil {
			return fmt.Errorf("write quarantine init script: %w", err)
		}
		gradleArgs = append(gradleArgs, "--init-script", initScriptPath)

		defer func() {
			logger.Println()
			logger.Printf("Removing test quarantine init script: %s", initScriptPath)
			if err := os.RemoveAll(initScriptPath); err != nil {
				logger.Warnf("failed to remove skip testing init script (%s): %s", initScriptPath, err)
			}
		}()
	}

	gradleExec := filepath.Join(cfg.ProjectLocation, cfg.GradlewPath)
	opts := command.Opts{
		Dir:    cfg.ProjectLocation,
		Stdout: os.Stdout,
		Stderr: os.Stderr,
	}
	maxAttempts := cfg.AllowedRetries + 1
	var lastTestErr error

	for attempt := 1; attempt <= maxAttempts; attempt++ {
		runCmd := cmdFactory.Create(gradleExec, gradleArgs, &opts)

		logger.Println()
		logger.Infof("Run Gradle (attempt %d/%d):", attempt, maxAttempts)
		logger.Donef("$ " + runCmd.PrintableCommandArgs())

		started := time.Now()
		testErr := runCmd.Run()
		if testErr != nil {
			logger.Errorf("Gradle command failed (attempt %d/%d): %v", attempt, maxAttempts, testErr)
		} else {
			logger.Donef("Gradle command finished successfully")
		}

		if err := exportResults(cfg, gradleProject, exporter, started, attempt, maxAttempts, logger); err != nil {
			return fmt.Errorf("export test outputs after attempt %d: %w", attempt, err)
		}

		if testErr == nil {
			return nil
		}

		lastTestErr = testErr
		if attempt < maxAttempts {
			logger.Println()
			logger.Warnf("Retrying Gradle command (%d retries remaining)...", maxAttempts-attempt)
		}
	}

	return fmt.Errorf("running tests failed after %d attempt(s): %w", maxAttempts, lastTestErr)
}

func exportResults(
	cfg configs,
	gradleProject gradle.Project,
	exporter output.Exporter,
	started time.Time,
	attempt int,
	maxAttempts int,
	logger log.Logger,
) error {
	attemptSuffix := ""
	if maxAttempts > 1 {
		attemptSuffix = fmt.Sprintf("-attempt-%d", attempt)
	}

	logger.Println()
	logger.Infof("Export HTML results:")

	reports, err := getArtifacts(gradleProject, started, cfg.HTMLResultDirs, true, true, logger)
	if err != nil {
		return fmt.Errorf("export HTML: %w", err)
	}
	reports = artifactsWithNameSuffix(reports, attemptSuffix)

	if err := exporter.ExportArtifacts(cfg.DeployDir, reports); err != nil {
		return fmt.Errorf("export HTML artifacts: %w", err)
	}

	logger.Println()
	logger.Infof("Export XML results:")

	results, err := getArtifacts(gradleProject, started, cfg.XMLResultDirs, true, true, logger)
	if err != nil {
		return fmt.Errorf("export XML dirs: %w", err)
	}
	results = artifactsWithNameSuffix(results, attemptSuffix)

	if err := exporter.ExportArtifacts(cfg.DeployDir, results); err != nil {
		return fmt.Errorf("export XML artifacts: %w", err)
	}

	if cfg.TestResultDir != "" {
		logger.Println()
		logger.Infof("Export XML results for test addon:")

		xmlPattern := cfg.XMLResultDirs
		if !strings.HasSuffix(xmlPattern, "*.xml") {
			xmlPattern += "*.xml"
		}

		resultXMLs, err := getArtifacts(gradleProject, started, xmlPattern, false, false, logger)
		if err != nil {
			logger.Warnf("find test addon XML files: %s", err)
		} else {
			exported, err := exporter.ExportTestAddonArtifacts(cfg.TestResultDir, resultXMLs, attemptSuffix)
			if err != nil {
				logger.Warnf("export test addon artifacts: %s", err)
			}
			if err := exporter.ExportFlakyTestsEnvVar(exported); err != nil {
				logger.Warnf("export flaky env var: %s", err)
			}
		}
	}

	return nil
}

func artifactsWithNameSuffix(artifacts []gradle.Artifact, suffix string) []gradle.Artifact {
	if suffix == "" {
		return artifacts
	}

	renamed := make([]gradle.Artifact, 0, len(artifacts))
	for _, artifact := range artifacts {
		updated := artifact
		updated.Name += suffix
		renamed = append(renamed, updated)
	}
	return renamed
}

func getArtifacts(
	gradleProject gradle.Project,
	started time.Time,
	pattern string,
	includeModuleName bool,
	isDirectoryMode bool,
	logger log.Logger,
) (artifacts []gradle.Artifact, err error) {
	for _, t := range []time.Time{started, {}} {
		if isDirectoryMode {
			artifacts, err = gradleProject.FindDirs(t, pattern, includeModuleName)
		} else {
			artifacts, err = gradleProject.FindArtifacts(t, pattern, includeModuleName)
		}
		if err != nil {
			return
		}
		if len(artifacts) == 0 {
			if t == started {
				logger.Warnf("No artifacts found with pattern: %s that has modification time after: %s", pattern, t)
				logger.Warnf("Retrying without modtime check...")
				logger.Println()
				continue
			}
			logger.Warnf("No artifacts found with pattern: %s without modtime check", pattern)
			logger.Warnf("If you changed report output dirs in Gradle, adjust report_path_pattern / result_path_pattern.")
		}
	}
	return
}

func conflictsWithQuarantine(gradleArgs []string) bool {
	for _, arg := range gradleArgs {
		if strings.HasPrefix(arg, "--init-script") || strings.HasPrefix(arg, "-I") {
			return true
		}
	}
	return false
}

func parseQuarantinedTests(input string) ([]string, error) {
	if strings.TrimSpace(input) == "" {
		return nil, nil
	}
	quarantinedTests, err := testquarantine.ParseQuarantinedTests(input)
	if err != nil {
		return nil, err
	}
	var skippedTests []string
	for _, qt := range quarantinedTests {
		if qt.ClassName == "" || qt.TestCaseName == "" {
			continue
		}
		skippedTests = append(skippedTests, fmt.Sprintf("%s.%s", qt.ClassName, qt.TestCaseName))
	}
	return skippedTests, nil
}
