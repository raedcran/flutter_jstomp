import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:jstomp/jstomp.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Stomp Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Stomp Example'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  JStomp stomp;

  String _initState = "";
  String _connectionState = "";
  String _subscriberState = "";
  String _content = "";
  String _sendContent = "";

  @override
  void initState() {
    super.initState();
    stomp = JStomp.instance;
//    _initStomp();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "Subscription status：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _initState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "Connection Status：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _connectionState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "Subscription status：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _subscriberState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "New message：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _content ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "Sent message：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _sendContent ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            RaisedButton(
              onPressed: () {
                _initStomp();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("Initialize and open the connection"),
            ),
            RaisedButton(
              onPressed: () {
                _sendMsg();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("Send a message"),
            ),
            RaisedButton(
              onPressed: () {
                _destroyStomp();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("Disconnect and destroy resources"),
            ),
          ],
        ),
      ),
    );
  }

  void _initStateChanged(String str) {
    setState(() {
      _initState = str;
    });
  }

  void _connectionStateChanged(String state) {
    setState(() {
      _connectionState = state;
    });
  }

  void _messageStateChanged(String msg) {
    setState(() {
      _content = msg;
    });
  }

  void _sendStateChanged(String send) {
    setState(() {
      _sendContent = send;
    });
  }

  ///
  ///Initialize and connect to stomp
  ///
  Future _initStomp() async {
    if (stomp == null) {
      stomp = JStomp.instance;
    }
    String userId = "1049236705720270849";
    String url =
        "ws://10.168.31.223:9080/message/apk-websocket?personId=" + userId;

    bool b =
        await stomp.init(url: url, sendUrl: "/microGroupMessage/sendMessage");

    _initStateChanged(b ? "Initialization successful" : "initialization failed");

    if (b) {
      ///Open connection
      await stomp.connection((open) async {
        print("The connection is open...$open");
        _connectionStateChanged("Stomp connection is open...");

        ///Subscribe to the peer-to-peer channel
        final String p2p = "/microGroupMessage/" + userId;
        bool b = await stomp.subscribP2P([p2p]);
        if (b) {
          setState(() {
            _subscriberState = "Channel subscription completed：" + p2p;
          });
        }
      }, onError: (error) {
        print("Connection open error...$error");
        _connectionStateChanged("Stomp connection is wrong：$error");
      }, onClosed: (closed) {
        print("Connection open error...$closed");
        _connectionStateChanged("Stomp connection is closed...");
      });
    }

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
      "content": "Message sent by flutter",
      "createId": "1143077861691756546",
      "createName": "陈晨",
      "createTime": "2019-06-24 17:03:51",
      "id": "1046324312976343042",
      "microGroupId": "1143049991384731649",
      "microGroupName": "Flutter discussion group",
      "type": 0
    };

    Map<String, dynamic> head = {
      "userId": "p123456",
      "token": "MgjkjkdIdkkDkkjkfdjfdkjfk",
    };
    return await stomp.sendMessage(json.encode(msg), header: head);
  }

  ///
  /// Disconnect and destroy resources
  ///
  Future<bool> _destroyStomp() async {
    if (stomp == null) {
      return true;
    }
    bool b = await stomp.destroy();
    stomp = null;
    return b;
  }
}
