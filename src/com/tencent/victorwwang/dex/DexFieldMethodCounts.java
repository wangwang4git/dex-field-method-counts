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

import com.android.dexdeps.*;

import java.io.PrintStream;
import java.util.*;

public class DexFieldMethodCounts {
    private static final PrintStream out = System.out;
    public int overallFieldCount = 0;
    public int overallMethodCount = 0;
    private final OutputStyle outputStyle;
    private final Node packageFieldTree;
    private final Map<String, IntHolder> packageFieldCount;
    private final Node packageMethodTree;
    private final Map<String, IntHolder> packageMethodCount;

    public final Map<String, IntPair> packageCount;

    DexFieldMethodCounts(OutputStyle outputStyle) {
        this.outputStyle = outputStyle;
        packageFieldTree = this.outputStyle == OutputStyle.TREE ? new Node() : null;
        packageFieldCount = this.outputStyle == OutputStyle.FLAT
                ? new TreeMap<String, IntHolder>() : null;
        packageMethodTree = this.outputStyle == OutputStyle.TREE ? new Node() : null;
        packageMethodCount = this.outputStyle == OutputStyle.FLAT
                ? new TreeMap<String, IntHolder>() : null;

        packageCount = new TreeMap<String, IntPair>();
    }

    // Mutable int holder
    public static class IntHolder {
        int value;
    }


    public static class IntPair {
        public int first;
        public int second;

        public IntPair() {
        }

        public IntPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IntPair intPair = (IntPair) o;

//            if (first != intPair.first) return false;
//            return second == intPair.second;
            if (Math.abs(first - intPair.first) > 2)  return false;
            return Math.abs(first - intPair.first) <= 2;

        }

        @Override
        public int hashCode() {
            int result = first;
            result = 31 * result + second;
            return result;
        }
    }

    enum Filter {
        ALL,
        DEFINED_ONLY,
        REFERENCED_ONLY
    }

    enum OutputStyle {
        TREE {
            @Override
            void output(DexFieldMethodCounts counts) {
                System.out.println("========== fields ==========");
                counts.packageFieldTree.output("");

                System.out.println("========== methods ==========");
                counts.packageMethodTree.output("");
            }
        },
        FLAT {
            @Override
            void output(DexFieldMethodCounts counts) {
//                System.out.println("========== fields ==========");
//                for (Map.Entry<String, IntHolder> e : counts.packageFieldCount.entrySet()) {
//                    String packageName = e.getKey();
//                    if (packageName == "") {
//                        packageName = "<no package>";
//                    }
//                    System.out.printf("%6s %s\n", e.getValue().value, packageName);
//                }
//
//                System.out.println("========== methods ==========");
//                for (Map.Entry<String, IntHolder> e : counts.packageMethodCount.entrySet()) {
//                    String packageName = e.getKey();
//                    if (packageName == "") {
//                        packageName = "<no package>";
//                    }
//                    System.out.printf("%6s %s\n", e.getValue().value, packageName);
//                }

                System.out.println("fields\t\tmethods\t\tpackage/class name");
//                for (Map.Entry<String, IntHolder> e : counts.packageFieldCount.entrySet()) {
//                    String packageName = e.getKey();
//
//                    int menthods = 0;
//                    if (counts.packageMethodCount.get(packageName) != null) {
//                        menthods = counts.packageMethodCount.get(packageName).value;
//                        counts.packageMethodCount.remove(packageName);
//                    }
//
//                    if (packageName == "") {
//                        packageName = "<no package>";
//                    }
//
//                    System.out.printf("%s\t\t%s\t\t%s\n", e.getValue().value, menthods, packageName);
//                }
//
//                for (Map.Entry<String, IntHolder> e : counts.packageMethodCount.entrySet()) {
//                    String packageName = e.getKey();
//                    System.out.printf("%s\t\t%s\t\t%s\n", 0, e.getValue().value, packageName);
//                }

                for (Map.Entry<String, IntPair> e : counts.packageCount.entrySet()) {
                    String packageName = e.getKey();
                    System.out.printf("%s\t\t%s\t\t%s\n", e.getValue().first, e.getValue().second, packageName);
                }
            }
        };

        abstract void output(DexFieldMethodCounts counts);
    }

    void output() {
        outputStyle.output(this);
    }

    int getOverallFieldCount() {
        return overallFieldCount;
    }

    int getOverallMethodCount() {
        return overallMethodCount;
    }

    public void calcPackageCount() {
        for (Map.Entry<String, IntHolder> e : packageFieldCount.entrySet()) {
            String packageName = e.getKey();

            int menthods = 0;
            if (packageMethodCount.get(packageName) != null) {
                menthods = packageMethodCount.get(packageName).value;
                packageMethodCount.remove(packageName);
            }

            if (packageName == "") {
                packageName = "<no package>";
            }

            packageCount.put(packageName, new IntPair(e.getValue().value, menthods));
        }

        for (Map.Entry<String, IntHolder> e : packageMethodCount.entrySet()) {
            String packageName = e.getKey();
            packageCount.put(packageName, new IntPair(0, e.getValue().value));
        }
    }

    public void putPackageCount(Map<String, IntPair> map) {
        for (Map.Entry<String, IntPair> e : map.entrySet()) {
            String packageName = e.getKey();

            if (packageCount.get(packageName) == null) {
                packageCount.put(packageName, e.getValue());
            } else {
                IntPair pair = new IntPair();
                pair.first = e.getValue().first + packageCount.get(packageName).first;
                pair.second = e.getValue().second + packageCount.get(packageName).second;
                packageCount.put(packageName, pair);
            }
        }
    }

    private static class Node {
        int count = 0;
        NavigableMap<String, Node> children = new TreeMap<String, Node>();

        void output(String indent) {
            if (indent.length() == 0) {
                out.println("<root>: " + count);
            }
            indent += "    ";
            for (String name : children.navigableKeySet()) {
                Node child = children.get(name);
                out.println(indent + name + ": " + child.count);
                child.output(indent);
            }
        }
    }

    public void generate(
            DexData dexData, boolean includeClasses,
            String packageFilter, int maxDepth, Filter filter) {
        MethodRef[] methodRefs = getMethodRefs(dexData, filter);

        for (MethodRef methodRef : methodRefs) {
            String classDescriptor = methodRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            overallMethodCount++;
            if (outputStyle == OutputStyle.TREE) {
                String packageNamePieces[] = packageName.split("\\.");
                Node packageNode = packageMethodTree;
                for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                    packageNode.count++;
                    String name = packageNamePieces[i];
                    if (packageNode.children.containsKey(name)) {
                        packageNode = packageNode.children.get(name);
                    } else {
                        Node childPackageNode = new Node();
                        if (name.length() == 0) {
                            // This method is declared in a class that is part of the default package.
                            // Typical examples are methods that operate on arrays of primitive data types.
                            name = "<default>";
                        }
                        packageNode.children.put(name, childPackageNode);
                        packageNode = childPackageNode;
                    }
                }
                packageNode.count++;
            } else if (outputStyle == OutputStyle.FLAT) {
                IntHolder count = packageMethodCount.get(packageName);
                if (count == null) {
                    count = new IntHolder();
                    packageMethodCount.put(packageName, count);
                }
                count.value++;
            }
        }
    }

    public void generate2(
            DexData dexData, boolean includeClasses,
            String packageFilter, int maxDepth, Filter filter) {
        FieldRef[] fieldRefs = getFieldRefs(dexData);

        for (FieldRef fieldRef : fieldRefs) {
            String classDescriptor = fieldRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            overallFieldCount++;
            if (outputStyle == OutputStyle.TREE) {
                String packageNamePieces[] = packageName.split("\\.");
                Node packageNode = packageFieldTree;
                for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                    packageNode.count++;
                    String name = packageNamePieces[i];
                    if (packageNode.children.containsKey(name)) {
                        packageNode = packageNode.children.get(name);
                    } else {
                        Node childPackageNode = new Node();
                        if (name.length() == 0) {
                            // This method is declared in a class that is part of the default package.
                            // Typical examples are methods that operate on arrays of primitive data types.
                            name = "<default>";
                        }
                        packageNode.children.put(name, childPackageNode);
                        packageNode = childPackageNode;
                    }
                }
                packageNode.count++;
            } else if (outputStyle == OutputStyle.FLAT) {
                IntHolder count = packageFieldCount.get(packageName);
                if (count == null) {
                    count = new IntHolder();
                    packageFieldCount.put(packageName, count);
                }
                count.value++;
            }
        }

        MethodRef[] methodRefs = getMethodRefs(dexData, filter);

        for (MethodRef methodRef : methodRefs) {
            String classDescriptor = methodRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            overallMethodCount++;
            if (outputStyle == OutputStyle.TREE) {
                String packageNamePieces[] = packageName.split("\\.");
                Node packageNode = packageMethodTree;
                for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                    packageNode.count++;
                    String name = packageNamePieces[i];
                    if (packageNode.children.containsKey(name)) {
                        packageNode = packageNode.children.get(name);
                    } else {
                        Node childPackageNode = new Node();
                        if (name.length() == 0) {
                            // This method is declared in a class that is part of the default package.
                            // Typical examples are methods that operate on arrays of primitive data types.
                            name = "<default>";
                        }
                        packageNode.children.put(name, childPackageNode);
                        packageNode = childPackageNode;
                    }
                }
                packageNode.count++;
            } else if (outputStyle == OutputStyle.FLAT) {
                IntHolder count = packageMethodCount.get(packageName);
                if (count == null) {
                    count = new IntHolder();
                    packageMethodCount.put(packageName, count);
                }
                count.value++;
            }
        }
    }

    private static MethodRef[] getMethodRefs(DexData dexData, Filter filter) {
        MethodRef[] methodRefs = dexData.getMethodRefs();
//        out.println("Read in " + methodRefs.length + " method IDs.");
        if (filter == Filter.ALL) {
            return methodRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        out.println("Read in " + externalClassRefs.length +
                " external class references.");
        Set<MethodRef> externalMethodRefs = new HashSet<MethodRef>();
        for (ClassRef classRef : externalClassRefs) {
            Collections.addAll(externalMethodRefs, classRef.getMethodArray());
        }
        out.println("Read in " + externalMethodRefs.size() +
                " external method references.");
        List<MethodRef> filteredMethodRefs = new ArrayList<MethodRef>();
        for (MethodRef methodRef : methodRefs) {
            boolean isExternal = externalMethodRefs.contains(methodRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal) ||
                    (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredMethodRefs.add(methodRef);
            }
        }
        out.println("Filtered to " + filteredMethodRefs.size() + " " +
                (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") +
                " method IDs.");
        return filteredMethodRefs.toArray(
                new MethodRef[filteredMethodRefs.size()]);
    }

    private static FieldRef[] getFieldRefs(DexData dexData) {
        FieldRef[] fieldRefs = dexData.getFieldRefs();
//        out.println("Read in " + fieldRefs.length + " field IDs.");
        return fieldRefs;
    }
}
