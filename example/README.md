# jstomp_example

  Currently only supports Android, let's take a look at the specific steps.
   1. initialization
   2. open the connection
   3. subscription channel
   4. add a monitor
   5. send a message

## Example

```
      ///Initialize and connect to stomp
      Future _initStomp() async {
        if (stomp == null) {
          stomp = JStomp.instance;
        }
        String userId = "104435390701569";
        String url = "ws://192.168.1.223:9990/message/websocket?personId=" + userId;
        bool b = await stomp.init(url: url, sendUrl: "/groupMessage/sendMessage");

        _initStateChanged(b ? "Initialization successful" : "initialization failed");

        if (b) {
          ///Open connection
          await stomp.connection((open) {
            print("The connection is open...$open");
            _connectionStateChanged("Stomp连接打Stomp connection is open开了...");
          }, onError: (error) {
            print("Connection open error...$error");
            _connectionStateChanged("Stomp connection is wrong：$error");
          }, onClosed: (closed) {
            print("Connection open error...$closed");
            _connectionStateChanged("Stomp connection is closed...");
          });
        }

        ///Subscribe to the peer-to-peer channel
        final String p2p = "/groupMessage/channel/" + userId;
        await stomp.subscribP2P([p2p]);

        ///Subscribe to the broadcast channel
        await stomp.subscribBroadcast(["groupBroadcast/message"]);

        setState(() {
          _subscriberState = "Channel subscription completed：" + p2p;
        });

        ///Add message callback
        await stomp.onMessageCallback((message) {
          print("Received p2p new message：" + message.toString());
          _messageStateChanged("Received p2p new message：" + message.toString());
        }, onBroadCast: (cast) {
          print("Received a new broadcast message：" + cast.toString());
          _messageStateChanged("Received a broadcast new message：" + cast.toString());
        });

        ///Add send callback
        await stomp.onSendCallback((status, sendMsg) {
          print("Message sent：$status :msg=" + sendMsg.toString());
          _sendStateChanged("Sent a message：$status :msg=" + sendMsg.toString());
        });
      }

      ///
      /// Send a message
      ///
      Future<String> _sendMsg() async {
        Map<String, dynamic> msg = {
          "content": "flutter发送的消息",
          "createId": "1143077861691756546",
          "createName": "陈晨",
          "createTime": "2019-06-24 17:03:51",
          "id": "1046324312976343042",
          "microGroupId": "1143049991384731649",
          "microGroupName": "flutter讨论群",
          "type": 0
        };

        Map<String, dynamic> head = {
          "userId": "p123456",
          "token": "MgjkjkdIdkkDkkjkfdjfdkjfk",
        };
        return await stomp.sendMessage(json.encode(msg), header: head);
      }

      /// Disconnect and destroy resources
      Future<bool> _destroyStomp() async {
        if (stomp == null) {
          return true;
        }
        bool b = await stomp.destroy();
        stomp = null;
        return b;
      }

```

The above is the basic steps used by the entire Stomp library.





















