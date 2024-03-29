







# Linux内核中网络IO的演变过程

* [1\. 初步了解BIO](#1-初步了解bio)
  * [1\.1 服务端](#11-服务端)
  * [1\.2 客户端](#12-客户端)
  * [1\.3 BIO在单线程情况下无法解决并发问题](#13-bio在单线程情况下无法解决并发问题)
    * [1\.3\.1 如何证明](#131-如何证明)
  * [1\.4 BIO在多线程情况下可以解决并发问题](#14-bio在多线程情况下可以解决并发问题)
    * [1\.4\.1 代码层面如何解决](#141-代码层面如何解决)
    * [1\.4\.2 存在的问题](#142-存在的问题)
* [2\. 理解NIO](#2-理解nio)
  * [2\.1 NIO的设计初衷](#21-nio的设计初衷)
  * [2\.2 代码层面实现](#22-代码层面实现)
  * [2\.3 问题的所在](#23-问题的所在)
  * [2\.3\.1 natvie方法在哪里](#231-natvie方法在哪里)
  * [2\.3\.2 重点分析](#232-重点分析)



## **网络IO的演变**

总体发展历程图，总体历程就是从BIO -> NIO -> NIO+多路复用(select,poll,epoll) -> AIO . 大致就是这张图，下文将详细说明。







# 1. 初步了解BIO

说起BIO，我们就来谈一下网络编程，其实网络编程，很多人学的都很差。网络编程的基本模型是C/S模型，即两个进程间的通信。服务端提供IP和监听端口，客户端通过连接操作向服务端监听的地址发起连接请求，通过三次握手连接，如果连接成功建立，双方就可以通过套接字进行通信。传统的同步阻塞模型开发中，ServerSocket负责绑定IP地址，启动监听端口；Socket负责发起连接操作。连接成功后，双方通过输入和输出流进行同步阻塞式通信。<br />

## 1.1 服务端

现在我们来写一个很简单的BIO实现的服务端。当我们把服务端启动的时候，服务端将会阻塞在下面的这行代码，目的是等待客户端来连接。<br />serverSocket.accept();<br />

<img src="img/2022-04-05_110841.png" align="left" style='width:800px'/>

## 1.2 客户端

当我们运行客户端，服务端将打印出客户端发来的数据，然后程序再次阻塞，等待客户端来连接，因为我们的服务端采用了while 循环。<br />

```
public class ClientApp {

    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(8080));
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("hello world \n".getBytes());
    }
}
```

<img src="img/2022-04-05_154516.png" align="left" style='width:800px'/>

## 1.3 BIO在单线程情况下无法解决并发问题

客户端假设如上面所写那样，你永远都体会不到一点，那就是服务端的以下代码也是阻塞的，即read方法是阻塞的。为什么这么说呢？上述那个客户端的例子一点都不好，可以说非常的差劲，但是几乎所有学习BIO编程的Java程序员都是从这个例子中学习过来的，我真想把最开始写这个例子程序的人摁到地上摩擦，简直是误人子弟。<br />inputStream.read(buffer);<br />我们来看一下，我把客户端的代码修改一下。让客户端连接上服务端的时候，不要立即去发数据，而是在那里等待一会，这样你就可以很明显的看到，服务端将会阻塞在read方法上面。不信你可以看看，当我们运行客户端的时候，服务端的控制台将会打印如下信息，这就足以说明服务端已经阻塞在read方法上面，等待客户端发送数据。<br />

```
public class ClientApp {

    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(8080));
        OutputStream outputStream = socket.getOutputStream();
        Scanner scan = new Scanner(System.in);
        String content = scan.nextLine();
        outputStream.write(content.getBytes());
    }
}
```

<img src="img/2022-04-05_155036.png" align="left" style='width:800px'/>

之后，我在客户端的控制台发送数据，随即你可以在服务端看到接收的数据，然后服务端打印出来之后，再次阻塞在accept方法上面，等待客户端的链接。<br />

<img src="img/2022-04-05_155743.png" align="left" style='width:800px'/>

然后我们再到服务端的控制台看下：<br />

<img src="img/2022-04-05_155942.png" align="left" style='width:800px'/>

通过以上分析，采用传统的BIO编程的话，会造成两个地方阻塞。第一个就是accept，等待客户端来连。第二个就是read，等待客户端发送数据。这两个阻塞会造成什么问题呢？那就决定了如果要让BIO实现并发，那么就必须要借助多线程。写到这里，可能还是有人不明白，我真的是醉了，好吧。如果你还不相信，那么下面我就用例子来证明这些理论，好吧。

### 1.3.1 如何证明

#### 1.3.1.1 证明一

第一步：我们启动服务端。那么服务端必须阻塞在accept方法上面，等待客户端来连。<br />第二步：我们启动客户端，连接到服务端之后，一直不发送数据，让服务端一直阻塞在read方法上面。如果能在服务端控制台看到如下信息，那就证明我们的客户端连接上来了。<br />第三步：我们再启动另一个客户端，发现怎么也连接不上服务端。因为服务端没有打印任何信息，目前服务端控制台出现的信息还是第一个客户端连接上来的时候打印的。那么这个服务端相当于瘫痪了，因为没有任何客户端可以连接上来了。直到我们的第一个客户端发数据上来了，第二个客户端才可以连接上。<br />第四步：第一个客户端发送数据给服务端。这个时候，你如果观察服务端的控制台。第一个客户端的数据已经接收到了，第二个客户端也连接上来了。<br />

#### 1.3.1.2 证明二

上面均是在Java代码层面进行的演示， 接下来我们将代码放到linux上面去,看看底层到底有哪些系统调用。

第一步：将代码迁移到linux系统上面去。

<img src="https://pengfeinie.github.io/images/2022-04-03_141226.png" align="left" style=' width:800px;height:100 px'/>

第二步：编译并执行ServerApp。注意到，这里使用strace命令进行追踪程序执行过程中的每一个的system call。

<img src="https://pengfeinie.github.io/images/2022-04-03_143637.png" align="left" style=' width:800px;height:100 px'/>

第三步：我们另开一个窗口，在当前目录下，在高版本的jdk中，第二个进程号对应的文件是主线程，可以看到如下内容。

<img src="https://pengfeinie.github.io/images/2022-04-03_143757.png" align="left" style=' width:800px;height:100 px'/>

第四步：我们可以看到此时服务端的Socket的状态是LISTEN。

<img src="https://pengfeinie.github.io/images/2022-04-03_150345.png" align="left" style=' width:800px;height:100 px'/>

第五步：我们通过vim out.13435察看文件内容。并通过 set nu进行行标的显示。

<img src="https://pengfeinie.github.io/images/2022-04-03_144358.png" align="left" style=' width:800px;height:100 px'/>

第六步：我们查找关键字waiting connecting。

<img src="https://pengfeinie.github.io/images/2022-04-03_145444.png" align="left" style=' width:800px;height:100 px'/>

第七步：使用tail -f out.13435进行日志追踪。

<img src="https://pengfeinie.github.io/images/2022-04-03_151544.png" align="left" style=' width:800px;height:100 px'/>

第八步：使用客户端进行连接。

<img src="https://pengfeinie.github.io/images/2022-04-03_152320.png" align="left" style=' width:800px;height:100 px'/>

<img src="https://pengfeinie.github.io/images/2022-04-03_152354.png" align="left" style=' width:800px;height:100 px'/>

这个时候我们发现out.13435文件内容发生了变化。

<img src="https://pengfeinie.github.io/images/2022-04-03_152511.png" align="left" style='width:800px'/>

我们可以看到此时服务端的Socket的状态。

<img src="https://pengfeinie.github.io/images/2022-04-03_153042.png" align="left" style='width:800px'/>

我们再启动另一个客户端，发现怎么也连接不上服务端。因为服务端没有打印任何信息，目前服务端控制台出现的信息还是第一个客户端连接上来的时候打印的。那么这个服务端相当于瘫痪了，因为没有任何客户端可以连接上来了。直到我们的第一个客户端发数据上来了，第二个客户端才可以连接上。

<img src="https://pengfeinie.github.io/images/2022-04-03_153713.png" align="left" style='width:800px'/>

现在我们让第一个客户端发送数据给服务端。

<img src="https://pengfeinie.github.io/images/2022-04-03_153910.png" align="left" style='width:800px'/>

<img src="https://pengfeinie.github.io/images/2022-04-03_153946.png" align="left" style='width:800px'/>

我们发现数据已经被服务端接收到了，同时第二个客户端已经连接上来了。

<img src="https://pengfeinie.github.io/images/2022-04-03_154122.png" align="left" style='width:800px'/>

这个时候我们发现out.13435文件内容发生了变化。

<img src="https://pengfeinie.github.io/images/2022-04-03_154316.png" align="left" style='width:800px'/>

说白了，BIO在单线程的情况下，是不能实现并发的，因为它在accept和read方法上面阻塞了。那如何解决呢？请看下面。

## 1.4 BIO在多线程情况下可以解决并发问题

采用 **BIO 通信模型** 的服务端，通常由一个独立的 Acceptor 线程负责监听客户端的连接。我们一般通过在`while(true)` 循环中，服务端会调用 `accept()` 方法等待接收客户端的连接的方式监听请求，一旦接收到一个连接请求，就可以建立通信套接字在这个通信套接字上进行读写操作，此时不能再接收其他客户端连接请求，只能等待同当前连接的客户端的操作执行完成， 不过可以通过多线程来支持多个客户端的连接，如下图所示。<br />如果要让 **BIO 通信模型** 能够同时处理多个客户端请求，就必须使用多线程（主要原因是`socket.accept()`、`socket.read()`、`socket.write()` 涉及的三个主要函数都是同步阻塞的），也就是说它在接收到客户端连接请求之后为每个客户端创建一个新的线程进行链路处理，处理完成之后，通过输出流返回应答给客户端，线程销毁。这就是典型的 **一请求一应答通信模型** 。<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392170-6b4f07bf-c23e-4186-abd9-67f1a00d1766.png#align=left&display=inline&height=469&margin=%5Bobject%20Object%5D&originHeight=469&originWidth=687&size=0&status=done&style=none&width=687)
<a name="BhKGY"></a>

### 1.4.1 代码层面如何解决

服务端采用多线程方式。如下所示：<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392152-aec4e8aa-6919-43fa-9590-3d595933efe4.jpeg#align=left&display=inline&height=410&margin=%5Bobject%20Object%5D&originHeight=410&originWidth=808&size=0&status=done&style=none&width=808)<br />客户端保持不变，因为能否解决并发问题，主要是在服务端。我们进行如下测试。<br />第一步：启动服务端。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392177-f504014d-c82d-4411-a228-04c8ce8304d8.jpeg#align=left&display=inline&height=359&margin=%5Bobject%20Object%5D&originHeight=359&originWidth=801&size=0&status=done&style=none&width=801)<br />第二步：启动第一个客户端。让这个客户端和服务端建立连接，但是不发送数据。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392179-e4885650-951e-4f56-a5ca-4fb55032ab6b.jpeg#align=left&display=inline&height=444&margin=%5Bobject%20Object%5D&originHeight=444&originWidth=792&size=0&status=done&style=none&width=792)<br />第三步：让另一个客户端和服务端建立连接。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392157-d9a0ac64-f623-4f76-b0ce-a75460889d7d.jpeg#align=left&display=inline&height=450&margin=%5Bobject%20Object%5D&originHeight=450&originWidth=795&size=0&status=done&style=none&width=795)<br />很明显，这个客户端显然连接上了服务端。此时我们发送数据看看。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392227-e91dab5d-8422-4b24-9f2d-f6ce9f10495b.jpeg#align=left&display=inline&height=488&margin=%5Bobject%20Object%5D&originHeight=488&originWidth=809&size=0&status=done&style=none&width=809)<br />从这里可以看出，BIO在多线程情况下，确实是可以解决并发问题的。
<a name="GRTiJ"></a>

### 1.4.2 存在的问题

我们可以设想一下，如果连接到服务端的客户端不做任何事情的话就会造成不必要的线程开销，不过我们可以通过 **线程池机制** 改善，线程池还可以让线程的创建和回收成本相对较低。使用`FixedThreadPool` 可以有效的控制了线程的最大数量，保证了系统有限的资源的控制，实现了N(客户端请求数量):M(处理客户端请求的线程数量)的I/O模型（N 可以远远大于 M），即BIO在多线程情况下可以解决并发问题。<br />**我们再设想一下当客户端并发访问量增加后这种模型会出现什么问题？**<br />在 Java 虚拟机中，线程是宝贵的资源，线程的创建和销毁成本很高，除此之外，线程的切换成本也是很高的。尤其在 Linux 这样的操作系统中，线程本质上就是一个进程，创建和销毁线程都是重量级的系统函数。如果并发访问量增加会导致线程数急剧膨胀可能会导致线程堆栈溢出、创建新线程失败等问题，最终导致进程宕机或者僵死，不能对外提供服务。<br />为了解决BIO面临的一个客户端请求需要一个线程处理的问题，后来有人对它的线程模型进行了优化一一一后端通过一个线程池来处理多个客户端的请求接入，形成客户端个数N：线程池最大线程数M的比例关系，其中N可以远远大于M。通过线程池可以灵活地调配线程资源，设置线程的最大值，防止由于海量并发接入导致线程耗尽。<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392204-33a15423-4366-42d4-81df-a5e9cb08eb97.png#align=left&display=inline&height=476&margin=%5Bobject%20Object%5D&originHeight=476&originWidth=711&size=0&status=done&style=none&width=711)<br />采用线程池和任务队列可以让BIO解决并发问题，它的模型图如上图所示。当有新的客户端接入时，将客户端的 Socket 封装成一个Task（该任务实现java.lang.Runnable接口）投递到后端的线程池中进行处理，JDK 的线程池维护一个消息队列和 N 个活跃线程，对消息队列中的任务进行处理。<br />由于线程池可以设置消息队列的大小和最大线程数，因此，它的资源占用是可控的，无论多少个客户端并发访问，都不会导致资源的耗尽和宕机。该方案采用了线程池实现，因此避免了为每个请求都创建一个独立线程造成的线程资源耗尽问题。不过因为它的底层仍然是同步阻塞的BIO模型，因此无法从根本上解决问题。在活动连接数不是特别高的情况下，这种模型是比较不错的，可以让每一个连接专注于自己的 I/O 并且编程模型简单，也不用过多考虑系统的过载、限流等问题。线程池本身就是一个天然的漏斗，可以缓冲一些系统处理不了的连接或请求。但是，当面对十万甚至百万级连接的时候，传统的 BIO 模型是无能为力的。因此，我们需要一种更高效的 I/O 处理模型来应对更高的并发量。
<a name="r4Npn"></a>

# 2. 理解NIO

<a name="D7VHn"></a>

## 2.1 NIO的设计初衷

基于BIO的缺陷，NIO被设计出来，就是为了在单线程情况下，可以解决并发问题。基于前面的分析，BIO之所以在单线程情况下不能解决并发问题的实质是accept和read方法都是阻塞的。那么很简单，NIO就是让这两个方法不阻塞不就可以了嘛。
<a name="NDEnF"></a>

## 2.2 代码层面实现

可以看到如下的代码，详细的注释已在代码中说明。例如：<br />第一步：当client1来连接的时候，accept方法获取连接立即返回，假设此时client1没有发数据过来的话，此时read方法也不会阻塞的。程序继续执行，再次进入while循环。<br />第二步：当再次进入while循环的时候，此时若还没有其他的客户端来连，那么此时accept方法立即返回null。<br />第三步：程序继续执行，若此时client1发来了数据，然后我们会发现，服务端已经接收不到数据了，因为我们已经丢失了client1的那个socket连接。那么怎么办呢？<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392184-f7333a47-d65d-4aa2-b657-af9ed95409a7.png#align=left&display=inline&height=571&margin=%5Bobject%20Object%5D&originHeight=571&originWidth=753&size=0&status=done&style=none&width=753)<br />针对上面的问题，我们如何改进呢？那么我们是不是要有一个集合来存这个已经连上来的客户端，是的，我们的分析是没有错的，那么请看下面的代码。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392213-7942d064-b89a-48f8-b17e-3a2ce7a204cf.jpeg#align=left&display=inline&height=498&margin=%5Bobject%20Object%5D&originHeight=498&originWidth=796&size=0&status=done&style=none&width=796)<br />我们有一个list集合来存储连接到服务端的客户端，如果有人连接上来，那么就放入到集合当中，随后遍历该集合，判断是否有客户端发送数据过来。如果没有人连接上来，那么也会遍历集合，目的是判断之前连接上来的客户端是否有发送数据过来。<br />我们启动服务端，并在客户端进行测试，如下：<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392214-307cb4c6-92dc-40d7-ace6-f0f721dc23b2.jpeg#align=left&display=inline&height=475&margin=%5Bobject%20Object%5D&originHeight=475&originWidth=804&size=0&status=done&style=none&width=804)<br />接着，我们再开另外一个客户端，发现也是可以连接上来的，并且第二个客户端发数据到服务端，服务端是可以接收到的，随后第一个客户端再发了一条数据，服务端同样是可以接收到的。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392219-1da2de52-80e8-419f-b029-d0f8c58268f3.jpeg#align=left&display=inline&height=360&margin=%5Bobject%20Object%5D&originHeight=360&originWidth=808&size=0&status=done&style=none&width=808)
<a name="aEUXR"></a>

## 2.3 问题的所在

<a name="LcJth"></a>

## 2.3.1 natvie方法在哪里

根据上面的分析，应用程序需要不断的去循环那个list集合。我们不应该将这个集合放在应用程序中去执行，应该放在操作系统的内核中去执行。那该怎么去解决呢？通过调用操作系统底层的select函数，就是将这个集合交给操作系统去循环。select是操作系统函数，可以实现该功能。那么我问你，Java是怎么调用操作系统函数呢？很简单，Java可以通过JNI方式调用native方法，而在native方法中去手动的调用操作系统的函数。不信你可以看我的分析。当你写这行代码的时候。<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392221-6d91a2d8-59e7-4cc1-aefd-ab361abe9769.png#align=left&display=inline&height=114&margin=%5Bobject%20Object%5D&originHeight=114&originWidth=593&size=0&status=done&style=none&width=593)<br />实际底层会调用如下的代码：<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392305-3d630121-9d31-44bb-b80c-e9d12ac5bad7.png#align=left&display=inline&height=495&margin=%5Bobject%20Object%5D&originHeight=495&originWidth=899&size=0&status=done&style=none&width=899)<br />即bind0方法，意为本地方法，即c语言实现的方法，那这个方法到底存在哪里呢？即JVM当中，那什么是JVM呢，说白了，就是你装好的那个java.exe程序，那么这个java.exe就是一个编译好的程序，那这个java.exe所属的项目到底是哪个呢？那就是openjdk, 源码的下载地址如下http://hg.openjdk.java.net/ ， 现在的问题是，我们只要下载好这个openjdk的源码，然后在源码里面去找这个bind0方法就好了。然后去看看bind0方法内部是如何实现的。<br />
<br />**windows平台中的实现**<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392228-0e7ac964-560e-4153-a61f-d0e670dad08a.jpeg#align=left&display=inline&height=445&margin=%5Bobject%20Object%5D&originHeight=445&originWidth=1080&size=0&status=done&style=none&width=1080)<br />我们打开这个文件，具体查看bind0到底是如何实现的。<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392277-7c3aefcd-0e68-4055-9d95-f22e9f1408f7.jpeg#align=left&display=inline&height=710&margin=%5Bobject%20Object%5D&originHeight=710&originWidth=894&size=0&status=done&style=none&width=894)<br />由于我们查看的是windows的bind0的实现，所以在源码中，你可以很清楚的看到调用了windows操作系统的函数。<br />
<br />**linux平台中的实现**<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392276-7063dbed-88c7-4491-94ea-ee1c30ff28ee.webp#align=left&display=inline&height=451&margin=%5Bobject%20Object%5D&originHeight=451&originWidth=1050&size=0&status=done&style=none&width=1050)<br />我们打开这个文件，具体查看bind0到底是如何实现的。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392261-68947bde-ae69-41d0-989f-f76a34b6a8b2.webp#align=left&display=inline&height=675&margin=%5Bobject%20Object%5D&originHeight=675&originWidth=991&size=0&status=done&style=none&width=991)
<a name="4bW7d"></a>

## 2.3.2 重点分析

**windows平台的分析**<br />当你调用这个方法的时候，底层是如何实现的呢？<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392235-7d1107db-6902-4536-b2ae-b30aacf088fb.webp#align=left&display=inline&height=93&margin=%5Bobject%20Object%5D&originHeight=93&originWidth=797&size=0&status=done&style=none&width=797)<br />追踪到源码，底层调用的代码如下：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392248-1be71893-075e-4ae6-954e-4fab0bc6185b.webp#align=left&display=inline&height=386&margin=%5Bobject%20Object%5D&originHeight=386&originWidth=631&size=0&status=done&style=none&width=631)<br />可以发现，在windows平台下，直接就new出来了一个WindowsSelectorProvider对象。分析到这里，你肯定也能猜到，在windows平台下，肯定是这个对象里面调用了native方法。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392230-73806da3-b7fd-4532-bda3-14c91a012a4a.webp#align=left&display=inline&height=396&margin=%5Bobject%20Object%5D&originHeight=396&originWidth=1080&size=0&status=done&style=none&width=1080)<br />即在这个poll0方法中，调用了windows平台下的select函数。说白了，在windows底层，就是调用了这个方法，将上述我们一直讨论的那个list集合交给操作系统的。接下来我们具体查看一下该函数的实现。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392207-2cc4317d-e81b-4379-9b34-0ef9b40ab739.webp#align=left&display=inline&height=521&margin=%5Bobject%20Object%5D&originHeight=521&originWidth=1080&size=0&status=done&style=none&width=1080)<br />我们打开这个文件，内容如下：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392245-e9f440d9-61fc-4554-9def-6f9e1be2fb7f.webp#align=left&display=inline&height=470&margin=%5Bobject%20Object%5D&originHeight=470&originWidth=1046&size=0&status=done&style=none&width=1046)<br />并且在poll0方法内部，显示的调用了select函数。如下所示：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392258-1b01e8be-2460-4656-98ee-9a6fd2887d85.webp#align=left&display=inline&height=497&margin=%5Bobject%20Object%5D&originHeight=497&originWidth=966&size=0&status=done&style=none&width=966)<br />
<br />**linux平台的分析**<br />当你调用这个方法的时候，底层是如何实现的呢？<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392244-fda1398a-215d-4bf5-83ba-968686d9f9dd.webp#align=left&display=inline&height=93&margin=%5Bobject%20Object%5D&originHeight=93&originWidth=797&size=0&status=done&style=none&width=797)<br />linux平台下追踪到源码，底层调用的代码如下：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392290-f024d8c0-de04-4ef1-a45e-81005de0e1e3.webp#align=left&display=inline&height=723&margin=%5Bobject%20Object%5D&originHeight=723&originWidth=1080&size=0&status=done&style=none&width=1080)<br />如果程序运行在Linux平台的话，就会采用<br />sun.nio.ch.EPollSelectorProvider<br />说明在高版本的JDK当中，linux平台中已经去除了select，采用了epoll。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392253-a72e5554-342c-436c-a142-4bc4f2f7d9a2.webp#align=left&display=inline&height=651&margin=%5Bobject%20Object%5D&originHeight=651&originWidth=1080&size=0&status=done&style=none&width=1080)<br />我们打开这个文件，进行查看。<br />![](https://cdn.nlark.com/yuque/0/2020/png/749466/1583991392333-866eeeaa-6017-4776-b6d7-3da89eb015d5.png#align=left&display=inline&height=507&margin=%5Bobject%20Object%5D&originHeight=507&originWidth=901&size=0&status=done&style=none&width=901)<br />
<br />**2.3.3 如何理解与查看系统函数**<br />接下来我给你看一下操作系统的函数。整个linux系统，你可以简单的理解为是c语言写的，那么既然是c语言写的，那么linux系统肯定对外提供了c语言函数，即当前操作系统的函数。<br />
<br />第一：查看socket函数。使用命令：man 2 socket<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392304-367bd69b-9bc7-4376-b851-1be66cc1f805.webp#align=left&display=inline&height=867&margin=%5Bobject%20Object%5D&originHeight=867&originWidth=824&size=0&status=done&style=none&width=824)<br />这个socket函数的意思，linux已经说的很清楚了。即：<br />socket - create an endpoint for communication <br />创建一个终端以用于进行通信。<br />
<br />第二：查看select函数。使用命令： man 2 select<br />![](https://cdn.nlark.com/yuque/0/2020/jpeg/749466/1583991392317-3423be03-b5d8-46d2-b180-9adfdbf2cb2c.jpeg#align=left&display=inline&height=728&margin=%5Bobject%20Object%5D&originHeight=728&originWidth=820&size=0&status=done&style=none&width=820)<br />
<br />谈到这个select函数，我们不得不提一下NIO的思想，就是说sun公司的那帮人是怎么想到将list集合交给操作系统去循环的。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392312-5d238cb4-ae72-495f-8bb4-04cdc2ce8844.webp#align=left&display=inline&height=519&margin=%5Bobject%20Object%5D&originHeight=519&originWidth=830&size=0&status=done&style=none&width=830)<br />接下来我们来自己写一个c语言程序，去调用linux系统函数。那么请看我如何写的，耐心一点好吧，我接下来就写了。<br />
<br />
<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392315-e6151fe7-78df-4430-ad5e-ff8ef2453523.webp#align=left&display=inline&height=390&margin=%5Bobject%20Object%5D&originHeight=390&originWidth=1080&size=0&status=done&style=none&width=1080)<br />
<br />运行如下命令进行编译：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392384-1310337e-2c5b-421b-9093-17a1efc4cc00.webp#align=left&display=inline&height=242&margin=%5Bobject%20Object%5D&originHeight=242&originWidth=726&size=0&status=done&style=none&width=726)<br />现在我们运行刚才编译好的程序，如下：<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392320-c4fe8b87-d834-47e6-af88-a3d24b7cd2f0.webp#align=left&display=inline&height=293&margin=%5Bobject%20Object%5D&originHeight=293&originWidth=742&size=0&status=done&style=none&width=742)<br />目前程序已经运行成功，正在等待连接。我们开一个客户端，运行命令：<br /> nc 127.0.0.1 9090<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392295-3400012f-5a32-403f-ba78-09edc88125fe.webp#align=left&display=inline&height=80&margin=%5Bobject%20Object%5D&originHeight=80&originWidth=1070&size=0&status=done&style=none&width=1070)<br />从上图可以看出，已经连接成功，之后我们在利用客户端向服务端发送一条数据，进行测试。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392345-e715af4b-22d8-4bf2-993c-78ebcec5aa01.webp#align=left&display=inline&height=121&margin=%5Bobject%20Object%5D&originHeight=121&originWidth=986&size=0&status=done&style=none&width=986)<br />以下是通过客户端工具进行测试。<br />![](https://cdn.nlark.com/yuque/0/2020/webp/749466/1583991392326-2e829aa8-894f-49b3-8d94-a058ae8f271d.webp#align=left&display=inline&height=541&margin=%5Bobject%20Object%5D&originHeight=541&originWidth=966&size=0&status=done&style=none&width=966)<br />到此，我们在linux平台写出了一个服务端。https://zhuanlan.zhihu.com/p/355582245

### 3. IO多路复用

Linux Socket 编程中I/O Multiplexing 主要通过三个函数来实现：select, poll,epoll来实现。I/O Multiplexing，先构造一张有关描述符的列表，然后调用一个函数，直到这些描述符中的一个已准备好进行I/O时，该函数才返回。在返回时，它告诉进程哪些描述符已准备好可以进行I/O。本文具体介绍一下select 和poll的用法，给出简单的demo代码，简要分析一下这两个函数的使用易出错的地方。https://www.linuxidc.com/Linux/2014-03/97444.htm
