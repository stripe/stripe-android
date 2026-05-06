package testaddon

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/bitrise-io/go-utils/v2/log"
)

const (
	// ResultDescriptorFileName is the name of the test result descriptor file.
	ResultDescriptorFileName = "test-info.json"
)

func ExportTestAddonArtifact(artifactPth, outputDir, fileNameSuffix string, lastOtherDirIdx int, logger log.Logger) (int, error) {
	dir := getExportDir(artifactPth)

	if dir == OtherDirName {
		// start indexing other dir name, to avoid overriding it
		// e.g.: other, other-1, other-2
		lastOtherDirIdx++
		if lastOtherDirIdx > 0 {
			dir = dir + "-" + strconv.Itoa(lastOtherDirIdx)
		}
	}

	artifactFileName := appendSuffixToFileName(filepath.Base(artifactPth), fileNameSuffix)
	if err := exportTestAddonArtifact(artifactPth, outputDir, dir, artifactFileName, logger); err != nil {
		return lastOtherDirIdx, err
	} else {
		src := artifactPth
		if rel, err := workDirRel(artifactPth); err == nil {
			src = "./" + rel
		}
		logger.Printf("Exporting %s => %s", src, filepath.Join("$BITRISE_TEST_RESULT_DIR", dir, artifactFileName))
	}
	return lastOtherDirIdx, nil
}

// exportTestAddonArtifact exports artifact found at path in directory uniqueDir, rooted at baseDir.
func exportTestAddonArtifact(path, testDeployDir, testName, fileName string, logger log.Logger) error {
	testResultDeployDir := filepath.Join(testDeployDir, testName)

	if err := os.MkdirAll(testResultDeployDir, os.ModePerm); err != nil {
		return fmt.Errorf("skipping artifact (%s): could not ensure unique export dir (%s): %s", path, testResultDeployDir, err)
	}

	if _, err := os.Stat(filepath.Join(testResultDeployDir, ResultDescriptorFileName)); os.IsNotExist(err) {
		m := map[string]string{"test-name": testName}
		data, err := json.Marshal(m)
		if err != nil {
			return fmt.Errorf("create test info descriptor: json marshal data (%s): %s", m, err)
		}
		if err := generateTestInfoFile(testResultDeployDir, data); err != nil {
			return fmt.Errorf("create test info descriptor: generate file: %s", err)
		}
	}

	if err := copyFile(path, filepath.Join(testResultDeployDir, fileName), logger); err != nil {
		return fmt.Errorf("failed to export artifact (%s), error: %v", fileName, err)
	}
	return nil
}

func appendSuffixToFileName(fileName, suffix string) string {
	if suffix == "" {
		return fileName
	}

	ext := filepath.Ext(fileName)
	base := strings.TrimSuffix(fileName, ext)
	return base + suffix + ext
}

func generateTestInfoFile(dir string, data []byte) error {
	f, err := os.Create(filepath.Join(dir, ResultDescriptorFileName))
	if err != nil {
		return err
	}

	if _, err := f.Write(data); err != nil {
		return err
	}

	if err := f.Sync(); err != nil {
		return err
	}

	if err := f.Close(); err != nil {
		return err
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

func copyFile(src, dst string, logger log.Logger) error {
	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer func() {
		if err := srcFile.Close(); err != nil {
			logger.Warnf("Failed to close source file (%s): %v", src, err)
		}
	}()

	dstFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer func() {
		if err := dstFile.Close(); err != nil {
			logger.Warnf("Failed to close destination file (%s): %v", dst, err)
		}
	}()

	_, err = io.Copy(dstFile, srcFile)
	if err != nil {
		return err
	}

	return nil
}
