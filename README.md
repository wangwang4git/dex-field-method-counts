# dex-field-method-counts
count fields and methods，diff two dex/apk/jar fields/methods

### 构建
```
cd $PROJECT_HOME
gradle assemble
```

构建`jar`，参见目录`$PROJECT_HOME/build/jar`  

### 执行
```
java -jar path\to\build\jar\dex-field-method-counts-1.0-SNAPSHOT.jar path\to\App.apk
```

### 输出
```
Processing ../project/master/trunk/bin/classes.dex
fields		methods		package/class name
0		3
29		21		AccostSvc
4		1		ActionMsg
13		4		AvatarInfo
22		0		EncounterSvc
0		1		GameCenter
6		2		GeneralSettings
60		8		IMMsgBodyPack
...
```

### 参数说明
```
"Usage: dex-field-method-counts [options] <file.{dex,apk,jar,directory}> ...\n" +
"Options:\n" +
"  --diff (need two <file.{dex,apk,jar}>)\n" +
"  --include-classes\n" +
"  --package-filter=com.foo.bar\n" +
"  --max-depth=N\n" +
"  --filter=ALL|DEFINED_ONLY|REFERENCED_ONLY\n" +
"  --output_style=FLAT|TREE\n"
```

* **--diff**: 支持两个apk输入，筛选输出两个apk之间有`fields`或者`methods`变动的package，输入如下：

```
Processing ../project/master/trunk/bin/XXXX_5.0.0_Android.apk
Overall field count: 178327
Overall method count: 166862
Processing ../project/fts/Android_6.1.0_XXXX/bin/XXXX_5.0.0_Android.apk
Overall field count: 178723
Overall method count: 167590
fields				methods				package/class name
    0|0			   74|69			android.animation
    2|2			  541|548			android.content
   82|82			  126|125			android.content.pm
    0|0			   95|99			android.database
   16|14			   87|85			android.hardware
   12|12			  280|278			android.media
...
Overall fields diff count: 396
Overall methods diff count: 728
```


The DEX file parsing is based on the `dexdeps` tool from
[the Android source tree](https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/).
