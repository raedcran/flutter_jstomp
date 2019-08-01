# jstomp

##### pub address：https://pub.dev/packages/jstomp

Previously, there was a websocket encapsulated in the Stomp protocol. The terminal used the form of a subscription channel, supported ws and http, and supported multiple channels. JStomp is a FLutter plugin that I developed based on the usage and summary of the current project. In small projects, there are message pushes, and IM and other business scenarios are added. Let's look at the capabilities provided by JStomp.

## First, function and characteristics

1. Support ws mode connection
        
2. Support http mode connection
        
3. Support custom authentication parameters when connecting, such as token, etc.
        
4. Support simultaneous subscription to multiple point-to-point channels
        
5. Support simultaneous subscription to multiple broadcast channels
        
6. Provide connection, message, send and other callback monitoring
        
7. Support for custom message headers when sending messages
        
8. JStomp is designed for singleton, avoiding multiple initializations and subscriptions
        
9. Can not manually disconnect, the program re-enters re-initialization processing, will not subscribe multiple times
        
10. Lightweight access, easy to use
        
11. Automatically manage the heartbeat, without the user to send the heartbeat to maintain a long connection
        
12. Support AndroidX
        
13. The connection fails or the connection is re-triggered for 15 minutes by default. The retry interval is 10 seconds.
        
        
## Second, how to use
    
#### 1、introduced in the pubspec.yaml file of the flutter project

```
dependencies:
    jstomp: ^1.1.3

import 'package:jstomp/jstomp.dart';

```
#### 2、initialize stomp

```
    JStomp stomp = JStomp.instance;

    //Initialize the url address of the connection
    String url="ws://192.168.3.25:9000/stompMsg.../...";

    //The url address of the sent message
    String sendUrl="sendMessage/android/...";

    //Initialize stomp, return true
    bool b =await stomp.init(url: url, sendUrl: sendUrl);

```

#### 3、open the connection

```
 if (b) {
      await stomp.connection((open) {

           print("The connection is open...$open");

      }, onError: (error) {

           print("Connection open error...$error");

      }, onClosed: (closed) {

           print("Connection open error...$closed");

      });
   }

   The parameter open: is a callback function with a bool type, true means that the connection is open normally, and false means failure.
    The parameter error: is a callback function with a String type parameter, indicating that the connection is wrong, and error is the error message;
    The parameter closed: is a callback function with a bool type, which means that the connection is closed and the return value is false;
```
             
#### 4、subscribe to the message channel, support peer-to-peer and broadcast, support simultaneous subscription to multiple channels

```
            //Point-to-point channel address, I subscribe to the channel with the specified userid here.
             final String p2p = "/microGroupMessage/" + userId;
             
             //Start a subscription
             await stomp.subscribP2P([p2p,"Address 2..."]);
             
             //Subscribe to the broadcast channel
             await stomp.subscribBroadcast(["Broadcast channel 1...","Broadcast channel 2..."]);

```

#### 5、set the message listener, callback when a new message arrives, you can set peer-to-peer and broadcast callback at the same time; the message returned by the callback is a string in json format,
           The json string can be parsed according to your needs.

```
           //Add a message listener
          await stomp.onMessageCallback((message) {//Point-to-point callback, mandatory parameter
           
                   print("Received p2p new message：" + message.toString());
                   
                 }, onBroadCast: (message) {     //Broadcast callback, optional parameters
                 
                   print("Received a new broadcast message：" + message.toString());
                   
                 });
           
           The parameter message: is a json string representing the content of the message received this time.
```

#### 6、set the send message callback listener, when sending a stomp message, regardless of whether the message is sent successfully or failed, this callback will be the content of this message
           The callback comes back, in addition to the send status.

```
           await stomp.onSendCallback((status, sendMsg) {
            
                 print("Message sent：$status :msg=" + sendMsg.toString());
                 
               });
            
            The parameter status: is an enumerated type，enum SendStatus { FAIL, SUCCESS }
            The parameter sendMsg: is a json string representing the content of the message sent this time.

```

#### 7、send a message

```
            //Use map to construct a data to be sent. The following data is the message data format on my project. The message field is customized according to your needs.
           Map<String, dynamic> msg = {
                 "content": "Message sent by flutter",
                 "createId": "161691756546",
                 "createName": "陈晨",
                 "createTime": "2019-06-24 17:03:51",
                 "id": "1046324312976343042",
                 "microGroupId": "1143049991384731649",
                 "microGroupName": "Flutter exclusive group",
                 "type": 0
               };
           
               
           //The first one: the default sending method, directly in the message content.
           //Start sending messages, this step must remember to be converted to json type       string, responsible for the format error will cause the transmission to fail, the underlying stomp only accepts data in json format
           
           await stomp.sendMessage(json.encode(msg)); 
           
           //Second: custom stomp header
           
           //Define a custom stomp header, which must be of type Map, and value only supports basic data types.
               Map<String, dynamic> head = {
                 "userId": "p123456",
                 "token": "MgjkjkdIdkkDkkjkfdjfdkjfk",
               };
           
           //Send a message, pass the custom header to the method parameter header
           await stomp.sendMessage(json.encode(msg), header: head);
```

#### 8、disconnect and destroy resources

```
     await stomp.destroy();

```




