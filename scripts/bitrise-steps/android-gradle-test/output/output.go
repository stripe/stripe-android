package output

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/bitrise-io/go-android/v2/gradle"
	"github.com/bitrise-io/go-android/v2/testresult/junitxml"
	"github.com/bitrise-io/go-steputils/v2/testreport"
	"github.com/bitrise-io/go-utils/v2/env"
	"github.com/bitrise-io/go-utils/v2/log"
	"github.com/bitrise-io/go-utils/v2/pathutil"
	"github.com/stripe/stripe-android/bitrise-step-gradle-test/testaddon"
)

const (
	flakyTestCasesEnvVarKey              = "BITRISE_FLAKY_TEST_CASES"
	flakyTestCasesEnvVarSizeLimitInBytes = 1024
)

type Exporter interface {
	ExportArtifacts(deployDir string, artifacts []gradle.Artifact) error
	ExportTestAddonArtifacts(testDeployDir string, artifacts []gradle.Artifact, fileNameSuffix string) ([]gradle.Artifact, error)
	ExportFlakyTestsEnvVar(artifacts []gradle.Artifact) error
}

type exporter struct {
	envRepository env.Repository
	pathChecker   pathutil.PathChecker
	logger        log.Logger
	converter     junitxml.Converter
}

func NewExporter(envRepository env.Repository, pathChecker pathutil.PathChecker, logger log.Logger) Exporter {
	return &exporter{
		envRepository: envRepository,
		pathChecker:   pathChecker,
		logger:        logger,
		converter:     junitxml.Converter{},
	}
}

func (e exporter) ExportArtifacts(deployDir string, artifacts []gradle.Artifact) error {
	for _, artifact := range artifacts {
		artifact.Name += ".zip"
		exists, err := e.pathChecker.IsPathExists(filepath.Join(deployDir, artifact.Name))
		if err != nil {
			return fmt.Errorf("failed to check path, error: %v", err)
		}

		if exists {
			timestamp := time.Now().Format("20060102150405")
			artifact.Name = fmt.Sprintf("%s-%s%s", strings.TrimSuffix(artifact.Name, ".zip"), timestamp, ".zip")
		}

		src := filepath.Base(artifact.Path)
		if rel, err := workDirRel(artifact.Path); err == nil {
			src = "./" + rel
		}

		e.logger.Printf("Exporting %s => $BITRISE_DEPLOY_DIR/%s", src, artifact.Name)

		if err := artifact.ExportZIP(deployDir); err != nil {
			e.logger.Warnf("failed to export artifact (%s), error: %v", artifact.Path, err)
			continue
		}
	}
	return nil
}

func (e exporter) ExportTestAddonArtifacts(testDeployDir string, artifacts []gradle.Artifact, fileNameSuffix string) ([]gradle.Artifact, error) {
	if len(artifacts) == 0 {
		return nil, nil
	}

	lastOtherDirIdx := -1
	var exportErrs []error
	var exportedArtifacts []gradle.Artifact

	for _, artifact := range artifacts {
		var err error
		lastOtherDirIdx, err = testaddon.ExportTestAddonArtifact(artifact.Path, testDeployDir, fileNameSuffix, lastOtherDirIdx, e.logger)
		if err != nil {
			exportErrs = append(exportErrs, fmt.Errorf("failed to export test addon artifact (%s): %w", artifact.Path, err))
		} else {
			exportedArtifacts = append(exportedArtifacts, artifact)
		}
	}

	if len(exportErrs) > 0 {
		errMsg := ""
		for _, err := range exportErrs {
			errMsg += fmt.Sprintf("- %s\n", err.Error())
		}
		return exportedArtifacts, fmt.Errorf("failed to export %d/%d test artifacts:\n%s", len(exportErrs), len(artifacts), errMsg)
	}

	return exportedArtifacts, nil
}

func (e exporter) ExportFlakyTestsEnvVar(artifacts []gradle.Artifact) error {
	if len(artifacts) == 0 {
		return nil
	}

	var exportErrs []error
	var flakyTestSuites []testreport.TestSuite

	for _, artifact := range artifacts {
		testReport, err := e.convertTestReport(artifact.Path)
		if err != nil {
			exportErrs = append(exportErrs, fmt.Errorf("failed to convert test report (%s): %w", artifact.Path, err))
			continue
		}

		testSuites := e.getFlakyTestSuites(testReport)
		flakyTestSuites = append(flakyTestSuites, testSuites...)
	}

	if err := e.exportFlakyTestCasesEnvVar(flakyTestSuites); err != nil {
		exportErrs = append(exportErrs, fmt.Errorf("failed to export flaky test cases env var: %w", err))
	}

	if len(exportErrs) > 0 {
		errMsg := ""
		for _, err := range exportErrs {
			errMsg += fmt.Sprintf("- %s\n", err.Error())
		}
		return fmt.Errorf("failed to export %d/%d test artifacts:\n%s", len(exportErrs), len(artifacts), errMsg)
	}

	return nil
}

func workDirRel(pth string) (string, error) {
	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}
	return filepath.Rel(wd, pth)
}

func (e exporter) convertTestReport(pth string) (testreport.TestReport, error) {
	if !e.converter.Detect([]string{pth}) {
		return testreport.TestReport{}, nil
	}

	testReport, err := e.converter.Convert()
	if err != nil {
		return testreport.TestReport{}, fmt.Errorf("failed to convert test report from %s: %w", pth, err)
	}

	return testReport, nil
}

func (e exporter) getFlakyTestSuites(testReport testreport.TestReport) []testreport.TestSuite {
	var flakyTestSuites []testreport.TestSuite

	for _, suite := range testReport.TestSuites {
		var flakyTests []testreport.TestCase
		testCasesToStatus := map[string]bool{}
		alreadySeenFlakyTests := map[string]bool{}

		for _, testCase := range suite.TestCases {
			testCaseID := testCase.ClassName + "." + testCase.Name

			newIsFailed := false
			if testCase.Failure != nil {
				newIsFailed = true
			}

			previousIsFailed, alreadySeen := testCasesToStatus[testCaseID]
			if !alreadySeen {
				testCasesToStatus[testCaseID] = newIsFailed
			} else {
				_, seen := alreadySeenFlakyTests[testCaseID]
				if !seen && (previousIsFailed != newIsFailed) {
					flakyTests = append(flakyTests, testreport.TestCase{
						XMLName:   testCase.XMLName,
						Name:      testCase.Name,
						ClassName: testCase.ClassName,
					})
					alreadySeenFlakyTests[testCaseID] = true
				}
			}
		}

		if len(flakyTests) > 0 {
			flakyTestSuites = append(flakyTestSuites, testreport.TestSuite{
				XMLName:   suite.XMLName,
				Name:      suite.Name,
				TestCases: flakyTests,
			})
		}
	}

	return flakyTestSuites
}

func (e exporter) exportFlakyTestCasesEnvVar(flakyTestSuites []testreport.TestSuite) error {
	if len(flakyTestSuites) == 0 {
		return nil
	}

	storedFlakyTestCases := map[string]bool{}
	var flakyTestCases []string

	for _, testSuite := range flakyTestSuites {
		for _, testCase := range testSuite.TestCases {
			testCaseName := testCase.Name
			if len(testCase.ClassName) > 0 {
				testCaseName = fmt.Sprintf("%s.%s", testCase.ClassName, testCase.Name)
			}

			if len(testSuite.Name) > 0 {
				testCaseName = testSuite.Name + "." + testCaseName
			}

			if _, stored := storedFlakyTestCases[testCaseName]; !stored {
				storedFlakyTestCases[testCaseName] = true
				flakyTestCases = append(flakyTestCases, testCaseName)
			}
		}
	}

	if len(flakyTestCases) > 0 {
		e.logger.Donef("%d flaky test case(s) detected, exporting %s env var", len(flakyTestCases), flakyTestCasesEnvVarKey)
	}

	var flakyTestCasesMessage string
	for i, flakyTestCase := range flakyTestCases {
		flakyTestCasesMessageLine := fmt.Sprintf("- %s\n", flakyTestCase)

		if len(flakyTestCasesMessage)+len(flakyTestCasesMessageLine) > flakyTestCasesEnvVarSizeLimitInBytes {
			e.logger.Warnf("%s env var size limit (%d characters) exceeded. Skipping %d test cases.", flakyTestCasesEnvVarKey, flakyTestCasesEnvVarSizeLimitInBytes, len(flakyTestCases)-i)
			break
		}

		flakyTestCasesMessage += flakyTestCasesMessageLine
	}

	if err := e.envRepository.Set(flakyTestCasesEnvVarKey, flakyTestCasesMessage); err != nil {
		return fmt.Errorf("failed to export %s: %w", flakyTestCasesEnvVarKey, err)
	}

	return nil
}
