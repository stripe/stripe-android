package gradleconfig

import (
	"bytes"
	"fmt"
	"path/filepath"
	"text/template"

	"github.com/bitrise-io/go-utils/v2/fileutil"
	"github.com/bitrise-io/go-utils/v2/pathutil"
)

const (
	testSkippingGradleInitScriptTemplateText = `allprojects {
    tasks.withType<Test>().configureEach {
        {{- range .ExcludedTests }}
        filter.excludeTestsMatching("{{ . }}")
        {{- end }}
    }
}`
)

type skipTestingTemplateData struct {
	ExcludedTests []string
}

func WriteSkipTestingInitScript(skipTesting []string) (string, error) {
	tmpDir, er := pathutil.NewPathProvider().CreateTempDir("gradle")
	if er != nil {
		return "", fmt.Errorf("create temp dir for Gradle init script: %w", er)
	}

	initScriptContent, err := generateTestSkippingGradleInitScriptContent(skipTesting)
	if err != nil {
		return "", fmt.Errorf("generate Gradle init script content: %w", err)
	}

	initGradlePath := filepath.Join(tmpDir, "bitrise-test-skipping.init.gradle.kts")
	err = fileutil.NewFileManager().Write(initGradlePath, initScriptContent, 0o755)
	if err != nil {
		return "", fmt.Errorf("write Gradle init script (%s): %w", initGradlePath, err)
	}

	return initGradlePath, nil
}

func generateTestSkippingGradleInitScriptContent(skipTesting []string) (string, error) {
	tmpl, err := template.New("bitrise-test-skipping.init.gradle.kts").Parse(testSkippingGradleInitScriptTemplateText)
	if err != nil {
		return "", err
	}

	resultBuffer := bytes.Buffer{}
	templateData := skipTestingTemplateData{ExcludedTests: skipTesting}
	if err := tmpl.Execute(&resultBuffer, templateData); err != nil {
		return "", err
	}

	return resultBuffer.String(), nil
}
