# 作业5 191840265 吴偲羿 用hadoop实现wordCount

## 1.程序基本设计思路

作业讲义中有关wordCount其实已经非常清楚，大概花费50行左右的代码就可以实现最最基本的对于一个文本的词频统计。但是 由于本次实验设计到1.多文本一起处理2.考虑标点符号，停词，数字与过短单词3.需要对输出进行排序，实际代码规模到达了300-400行。但是基本程序设计思路是没有变化的

主要的处理方法：

### 1.预处理

![image-20211030010542476](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030010542476.png)

![image-20211030010556335](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030010556335.png)

通过读取给定的停词，标点符号文件将需要跳过的单词写入有关列表中，以便之后词频统计时进行处理

### 2.正式词频统计（mapper）

我们通过TokenizerFileMapper打开输入文件夹，按照文件数量分配mapper，根据行 进行分词。由于需要分文件进行统计，设计时返回map的key用‘-‘连接了单词与所在文件，以便区分，如下：

这里与示例程序思路是没有差别的，除了在统计特定单词时，需要对其进行是否需要skip的判断 如下：

![image-20211030012541972](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030012541972.png)

其中

~~~java
!Pattern.compile("^[-\\+]?[\\d]*$").matcher(temp).matches()
~~~

是用正则表达式对单词是否是数字 的判断。

这里reduce函数与示例函数没有差别。 为了进行后面排序的处理，我们将此词频统计的结果输出至temp_part文件夹。作为下一任务 排序 的输入。

### 3.排序

通过上一步的mapreduce工作 我们获得了所有文件中的单词，单词所在文件以及该单词的词频。现在我们通过一个新的mapreduce任务对其进行排序。

我们了解 在hadoop中我们有job.setSortComparatorClass()设置的key比较函数类，描述如下：

![image-20211030015926832](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030015926832.png)

然而，如图所示，默认最后返回的key是排在最后的一个，而我们希望可以获得最大的词频，即降序排序，因此我们重写排序类：

![image-20211030020218754](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030020218754.png)

这里 compare前的‘-’号 是因为compare根据比较大小输出-1，0和1，通过‘-‘即可解决升序，降序问题。

这样，通过这个类，我们获得了之前对所有文件进行词频中，相同key(即相同文件中的相同单词)的最高value。下一步只需统计每个文件中前100的单词了。

我们定义一个hashmap<string,int>，来储存某一文件已输出的高频单词数，我们共需输出40个文件各100个单词，因此当hashmap中int的sum达到4000时，代表我们的任务就结束了。

这里我们需要先将key中的word与文件名拆分开：

![image-20211030020634433](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030020634433.png)

对于每一个docId，rank初始设置为1，每从一个docId中获得一个高频单词，就将该高频单词计数写入result并rank++，当rank达到100时，该文件统计结束，通过MultipleOutputs写入不同文件中，我们统一写入single-output文件夹。

![image-20211030021253548](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030021253548.png)

这是分文件的输出。

### 4.对于所有文件一起统计

和前面十分类似，唯一需要修改的是，mapper输出的key不需要体现单词所在的文件了，只需输出<word,one>即可：

![image-20211030021903308](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030021903308.png)

我们将其写入temp-all 文件夹，进行排序

同样的,在排序类里面，我们更改如下：

![image-20211030022040699](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030022040699.png)

并将结果写入output文件夹。

**这样，我们通过4个任务，分文件的统计、排序与总共的统计、排序，完成了程序要求的词频统计，下面展示运行结果**

首先向hdfs传入input文件

![image-20211030025933868](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030025933868.png)

执行程序：

![image-20211030022739332](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030022739332.png)

这里 我们向程序传入了4个参数 input output分别指定了输入输出文件夹， -skip后两个文件为停词的文本文件，在main函数相关处理如下：

![image-20211030022947955](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030022947955.png)

remainingArgs储存参数，remainingArgs[0],remainingArgs[1]保存输入输出路径，通过判断是否存在‘-skip’参数，决定是否需要停词。

![image-20211030030132619](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030030132619.png)

输出结果如下：

Output（总词频统计）

![image-20211030023144731](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030023144731.png)

single-output（单文件词频统计）（以李尔王为例）

![image-20211030023229061](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030023229061.png)

## 2.遇到的除代码以外一些问题

### 打包成jar包的时候报错 mkdirs failed to creat /var/...

![image-20211029213829923](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211029213829923.png)

解决方案：

mac os x打包hadoop的jar包报的错

zip -d mr.jar META-INF/LICENSE

输入如上指令 即可

### zsh指令突然全部失效

写着写着更新了macOS Monterey 然后少有的重启了一下电脑 再次打开terminal 发现zsh六亲不认：

**zsh:command not found: ls**

**zsh:command not found: vim**

...

查了一下好像是zsh普遍的问题 关键问题还是 zsh进来时候读的配置文件的问题

由于网上的教程几乎全是在bash shell里搞定的， 所以有些环境变量不知道为什么就 抽风了。

根据教程 执行PATH=/bin:/usr/bin:/usr/local/bin:${PATH}，指令是回来了 但是治标不治本。

查阅相关资料发现 zsh的环境变量是从~/.zshrc读取的，因此将环境变量生效的指令 source /etc/profile和export PATH指令写入.zshrc 问题解决。

![image-20211030023652517](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030023652517.png)

## 可以改进的地方

### 1.任务重复

由于本人比较懒（x 所以将统计全部词频与统计单文件词频分开来做

其实 在统计完单文件词频后 我们完全可以根据single-output中的文件，对所有词频再次进行一个mapreduce，或者在分文件进行排序时，根据之前统计词频的<word-filename,value>将word单独分开来进行排序，这样就不需要执行两次重复的词频统计力。

### 2.同义词合并/停词更新

也许这不是程序员需要考虑的问题 但是从程序的输出来看 确实有非常多的同义词占据了排行榜的位置：

![image-20211030024807029](/Users/quinton_541/Library/Application Support/typora-user-images/image-20211030024807029.png)

例如 thou/thy/thee（虽然我觉得这个词完全应该放在停词里

包括一些时态是否需要进行合并，这些都是可以优化的地方

当然，这也可以变成程序员考虑的问题，例如动词判断时态，将其规约到动词原形(doth,does,did,doing,done->do)，但是不规则动词似乎只能枚举 手打（有点折磨人

另外 停词列表是否需要更合理的进行更新，我们发现，在统计出的高频单词中，shall,doth,hath,art这些无意义的助词占据了许多位置（当然，中世纪英语里art=are，判断art到底是啥意思好像也是一个问题，这可能要涉及到NLP的东西）