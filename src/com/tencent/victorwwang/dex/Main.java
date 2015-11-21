/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.victorwwang.dex;

import com.android.dexdeps.DexData;
import com.android.dexdeps.DexDataException;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Main {
    private boolean diffMode;
    private boolean includeClasses;
    private String packageFilter;
    private int maxDepth = Integer.MAX_VALUE;
    private DexFieldMethodCounts.Filter filter = DexFieldMethodCounts.Filter.ALL;
    private DexFieldMethodCounts.OutputStyle outputStyle = DexFieldMethodCounts.OutputStyle.FLAT;

    public static class IntFour {
        public int first;
        public int second;
        public int three;
        public int four;

        public IntFour(int first, int second, int three, int four) {
            this.first = first;
            this.second = second;
            this.three = three;
            this.four = four;
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.run(args);
    }

    void run(String[] args) {
        try {
            String[] inputFileNames = parseArgs(args);
            if (diffMode) {
                // inputFileNames.length must 2
                DexFieldMethodCounts[] countDiff = new DexFieldMethodCounts[2];

                for (int i = 0; i < inputFileNames.length; ++i) {
                    System.out.println("Processing " + inputFileNames[i]);

                    countDiff[i] = new DexFieldMethodCounts(outputStyle);

                    for (String fileName : collectFileNames(inputFileNames[i])) {
                        DexFieldMethodCounts counts = new DexFieldMethodCounts(outputStyle);
                        List<RandomAccessFile> dexFiles = openInputFiles(fileName);

                        for (RandomAccessFile dexFile : dexFiles) {
                            DexData dexData = new DexData(dexFile);
                            dexData.load();
                            counts.generate2(dexData, includeClasses, packageFilter, maxDepth, filter);
                            dexFile.close();
                        }
                        counts.calcPackageCount();

                        countDiff[i].putPackageCount(counts.packageCount);
                        countDiff[i].overallFieldCount += counts.getOverallFieldCount();
                        countDiff[i].overallMethodCount = counts.getOverallMethodCount();
                    }

//                    countDiff[i].output();
                    System.out.println("Overall field count: " + countDiff[i].overallFieldCount);
                    System.out.println("Overall method count: " + countDiff[i].overallMethodCount);
                }

                // 求差
                TreeMap<String, IntFour> packageDiff = new TreeMap<String, IntFour>();
                for (Map.Entry<String, DexFieldMethodCounts.IntPair> e : countDiff[0].packageCount.entrySet()) {
                    String packageName = e.getKey();
                    DexFieldMethodCounts.IntPair pair0 = e.getValue();

                    if (countDiff[1].packageCount.get(packageName) != null) {
                        DexFieldMethodCounts.IntPair pair1 = countDiff[1].packageCount.get(packageName);
                        if (!pair0.equals(pair1)) {
                            packageDiff.put(packageName, new IntFour(pair0.first, pair1.first, pair0.second, pair1.second));
                        }

                        countDiff[1].packageCount.remove(packageName);
                    } else {
                        packageDiff.put(packageName, new IntFour(pair0.first, 0, pair0.second, 0));
                    }
                }
                for (Map.Entry<String, DexFieldMethodCounts.IntPair> e : countDiff[1].packageCount.entrySet()) {
                    String packageName = e.getKey();
                    DexFieldMethodCounts.IntPair pair1 = e.getValue();

                    packageDiff.put(packageName, new IntFour(0, pair1.first, 0, pair1.second));
                }

                // 输出
                if (packageDiff.size() > 0) {
                    System.out.println("fields\t\t\t\tmethods\t\t\t\tpackage/class name");

                    int fieldsDiff = 0;
                    int methodsDiff = 0;
                    for (Map.Entry<String, IntFour> e : packageDiff.entrySet()) {
                        String packageName = e.getKey();
                        IntFour pair = e.getValue();

                        fieldsDiff += -pair.first + pair.second;
                        methodsDiff += -pair.three + pair.four;
                        System.out.printf("%5s|%-5s\t\t\t%5s|%-5s\t\t\t%s\n", pair.first, pair.second, pair.three, pair.four, packageName);
                    }

                    System.out.println("Overall fields diff count: " + fieldsDiff);
                    System.out.println("Overall methods diff count: " + methodsDiff);
                } else {
                    System.out.println("fields & methods is same");
                }

            } else {
                int overallFieldCount = 0;
                int overallMethodCount = 0;
                for (String fileName : collectFileNames(inputFileNames)) {
                    System.out.println("Processing " + fileName);
                    DexFieldMethodCounts counts = new DexFieldMethodCounts(outputStyle);
                    List<RandomAccessFile> dexFiles = openInputFiles(fileName);

                    for (RandomAccessFile dexFile : dexFiles) {
                        DexData dexData = new DexData(dexFile);
                        dexData.load();
                        counts.generate2(dexData, includeClasses, packageFilter, maxDepth, filter);
                        dexFile.close();
                    }
                    counts.calcPackageCount();
                    counts.output();
                    overallFieldCount = counts.getOverallFieldCount();
                    overallMethodCount = counts.getOverallMethodCount();
                }
                System.out.println("Overall field count: " + overallFieldCount);
                System.out.println("Overall method count: " + overallMethodCount);
            }
        } catch (UsageException ue) {
            usage();
            System.exit(2);
        } catch (IOException ioe) {
            if (ioe.getMessage() != null) {
                System.err.println("Failed: " + ioe);
            }
            System.exit(1);
        } catch (DexDataException dde) {
            /* a message was already reported, just bail quietly */
            System.exit(1);
        }
    }

    /**
     * Opens an input file, which could be a .dex or a .jar/.apk with a
     * classes.dex inside.  If the latter, we extract the contents to a
     * temporary file.
     */
    List<RandomAccessFile> openInputFiles(String fileName) throws IOException {
        List<RandomAccessFile> dexFiles = new ArrayList<RandomAccessFile>();

        openInputFileAsZip(fileName, dexFiles);
        if (dexFiles.size() == 0) {
            File inputFile = new File(fileName);
            RandomAccessFile dexFile = new RandomAccessFile(inputFile, "r");
            dexFiles.add(dexFile);
        }

        return dexFiles;
    }

    /**
     * Tries to open an input file as a Zip archive (jar/apk) with a
     * "classes.dex" inside.
     */
    void openInputFileAsZip(String fileName, List<RandomAccessFile> dexFiles) throws IOException {
        ZipFile zipFile;

        // Try it as a zip file.
        try {
            zipFile = new ZipFile(fileName);
        } catch (FileNotFoundException fnfe) {
            // not found, no point in retrying as non-zip.
            System.err.println("Unable to open '" + fileName + "': " +
                    fnfe.getMessage());
            throw fnfe;
        } catch (ZipException ze) {
            // not a zip
            return;
        }

        // Open and add all files matching "classes.*\.dex" in the zip file.
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (entry.getName().matches("classes.*\\.dex")) {
                dexFiles.add(openDexFile(zipFile, entry));
            }
        }

        zipFile.close();
    }

    RandomAccessFile openDexFile(ZipFile zipFile, ZipEntry entry) throws IOException {
        // We know it's a zip; see if there's anything useful inside.  A
        // failure here results in some type of IOException (of which
        // ZipException is a subclass).
        InputStream zis = zipFile.getInputStream(entry);

        // Create a temp file to hold the DEX data, open it, and delete it
        // to ensure it doesn't hang around if we fail.
        File tempFile = File.createTempFile("dexdeps", ".dex");
        RandomAccessFile dexFile = new RandomAccessFile(tempFile, "rw");
        tempFile.delete();

        // Copy all data from input stream to output file.
        byte copyBuf[] = new byte[32768];
        int actual;

        while (true) {
            actual = zis.read(copyBuf);
            if (actual == -1)
                break;

            dexFile.write(copyBuf, 0, actual);
        }

        dexFile.seek(0);

        return dexFile;
    }

    private String[] parseArgs(String[] args) {
        int idx;

        for (idx = 0; idx < args.length; idx++) {
            String arg = args[idx];

            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            } else if (arg.equals("--diff")) {
                diffMode = true;
            } else if (arg.equals("--include-classes")) {
                includeClasses = true;
            } else if (arg.startsWith("--package-filter=")) {
                packageFilter = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("--max-depth=")) {
                maxDepth =
                        Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
            } else if (arg.startsWith("--filter=")) {
                filter = Enum.valueOf(
                        DexFieldMethodCounts.Filter.class,
                        arg.substring(arg.indexOf('=') + 1).toUpperCase());
            } else if (arg.startsWith("--output_style")) {
                outputStyle = Enum.valueOf(
                        DexFieldMethodCounts.OutputStyle.class,
                        arg.substring(arg.indexOf('=') + 1).toUpperCase());
            } else {
                System.err.println("Unknown option '" + arg + "'");
                throw new UsageException();
            }
        }

        // We expect at least one more argument (file name).
        int fileCount = args.length - idx;
        if (fileCount == 0) {
            throw new UsageException();
        }

        if (diffMode && fileCount != 2) {
            throw new UsageException();
        }

        String[] inputFileNames = new String[fileCount];
        System.arraycopy(args, idx, inputFileNames, 0, fileCount);
        return inputFileNames;
    }

    private void usage() {
        System.err.print(
                "DEX per-package/class field/method counts v1.0\n" +
                        "Usage: dex-field-method-counts [options] <file.{dex,apk,jar,directory}> ...\n" +
                        "Options:\n" +
                        "  --diff (need two <file.{dex,apk,jar}>)\n" +
                        "  --include-classes\n" +
                        "  --package-filter=com.foo.bar\n" +
                        "  --max-depth=N\n" +
                        "  --filter=ALL|DEFINED_ONLY|REFERENCED_ONLY\n" +
                        "  --output_style=FLAT|TREE\n"
        );
    }

    /**
     * Checks if input files array contain directories and
     * adds it's contents to the file list if so.
     * Otherwise just adds a file to the list.
     *
     * @return a List of file names to process
     */
    private List<String> collectFileNames(String[] inputFileNames) {
        List<String> fileNames = new ArrayList<String>();
        for (String inputFileName : inputFileNames) {
            File file = new File(inputFileName);
            if (file.isDirectory()) {
                String dirPath = file.getAbsolutePath();
                for (String fileInDir : file.list()) {
                    fileNames.add(dirPath + File.separator + fileInDir);
                }
            } else {
                fileNames.add(inputFileName);
            }
        }
        return fileNames;
    }

    private List<String> collectFileNames(String inputFileName) {
        List<String> fileNames = new ArrayList<String>();
        File file = new File(inputFileName);
        if (file.isDirectory()) {
            throw new UsageException();
        } else {
            fileNames.add(inputFileName);
        }
        return fileNames;
    }

    private static class UsageException extends RuntimeException {
    }
}
