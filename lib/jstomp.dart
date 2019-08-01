import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

///Define message callback functions and formulate parameter types
typedef OnMessageCallback = Function(dynamic jsonMsg);

///Define a message dispatch callback function
typedef OnSendMessageCallback = Function(SendStatus status, dynamic jsonMsg);

class JStomp {
  JStomp._();

  static JStomp _instance;

  factory JStomp() => _getInstance();

  static JStomp get instance => _getInstance();

  ///Channel instance
  MethodChannel _channel;

  ///Accept message stream
  StreamController<_OnMessageData> _messageController;

  ///Connect back to stream
  StreamController<_OnConnectionData> _connectionController;

  ///Send message data stream
  // ignore: close_sinks
  StreamController<_OnSendMessageData> _sendController;

  JStomp._init() {
    ///initialization
    _channel = const MethodChannel('jstomp');
    _connectionController = new StreamController.broadcast();
    _messageController = new StreamController.broadcast();
    _sendController = new StreamController.broadcast();
  }

  ///
  /// Return instance object
  ///
  static JStomp _getInstance() {
    if (_instance == null) {
      _instance = JStomp._init();
    }
    return _instance;
  }

  ///
  /// Stomp initialization
  ///
  Future<bool> init({@required String url, @required String sendUrl}) async {
    ///Add native method call processing method
    _channel.setMethodCallHandler(_nativeHandle);

    Map<String, String> params = {
      "url": url,
      "sendUrl": sendUrl,
    };
    bool result = await _channel.invokeMethod(_NativeMethod.INIT, params);
    return result;
  }

  ///
  /// Open stomp connection
  ///
  Future<bool> connection(ValueChanged onOpen,
      {ValueChanged onError, ValueChanged onClosed}) async {
    ///Register the connection status listener first
    _onConnectionCallback(onOpen, onError, onClosed);
    return await _channel.invokeMethod(_NativeMethod.CONNECTION);
  }

  ///
  /// Stomp connection listener
  ///
  void _onConnectionCallback(
      ValueChanged onOpen, ValueChanged onError, ValueChanged onClosed) {
    _connectionController.stream.listen((callback) {
      switch (callback.call) {
        case _Connection.OPEN:
          if (onOpen != null) {
            onOpen(callback.state);
          }
          break;
        case _Connection.ERROR:
          if (onError != null) {
            onError(callback.state);
          }
          break;
        case _Connection.CLOSED:
          if (onClosed != null) {
            onClosed(callback.state);
          }
          break;
      }
    });
  }

  ///
  /// Destroy
  /// Disconnect stomp and destroy all resources, including client, listener, Rxjava Observer, etc.
  /// Stop the service
  ///
  Future<bool> destroy() async {
    bool b = await _channel.invokeMethod(_NativeMethod.DESTROY);
    await _closedStreamControllers();
    return b;
  }

  ///
  /// Subscribe to the p2p channel
  /// [urls] The point-to-point channel address to be subscribed to, can be multiple
  ///
  Future<bool> subscribP2P(List<String> urls) async {
    assert(urls != null);
    String urlStr = urls.join(",");
    return _channel.invokeMethod(_NativeMethod.SUBSCRIBER_P2P, urlStr);
  }

  ///
  /// Subscribe to the broadcast channel
  /// [urls] The broadcast channel address to be subscribed to, can be multiple
  ///
  Future<bool> subscribBroadcast(List<String> urls) async {
    assert(urls != null);
    String urlStr = urls.join(",");
    return await _channel.invokeMethod(
        _NativeMethod.SUBSCRIBER_BROADCAST, urlStr);
  }

  ///
  /// Accept message listener
  /// [onMessage] Point-to-point message back to function
  /// [onBroadCast] broadcast message callback function
  ///
  Future<bool> onMessageCallback(OnMessageCallback onMessage,
      {OnMessageCallback onBroadCast}) async {
    ///Listening for message flow
    _messageController.stream.listen((message) {
      ///Callback flutter based on the specific message type
      switch (message.type) {
        case _MessageType.P2P: //Peer-to-peer message
          onMessage(message.message);
          break;
        case _MessageType.BROADCAST: //Broadcast message
          if (onBroadCast != null) {
            onBroadCast(message.message);
          }
          break;
        default:
          break;
      }
    });

    ///Call the native method to register the message callback
    return _channel.invokeMethod(_NativeMethod.MESSAGE_CALLBACK);
  }

  ///
  /// Send a message
  /// [message] message body, usually json
  /// [header] stomp message header, the default can not pass
  ///
  Future<String> sendMessage(String message, {Map<String, dynamic> header}) {
    ///Convert the value of the stomp header to a String type.
    Map<String, String> headMap = new Map();
    if (header != null) {
      headMap = header.map((String key, value) {
        return new MapEntry(key, value.toString());
      });
    }
    Map<String, dynamic> params = {"msg": message, "header": headMap};
    return _channel.invokeMethod(_NativeMethod.SEND_MESSAGE, params);
  }

  ///
  /// Send a message listener
  /// [callback] Send a message callback function with the parameter body
  ///
  Future<bool> onSendCallback(OnSendMessageCallback callback) async {
    _sendController.stream.listen((message) {
      if (message.status == 1) {
        callback(SendStatus.SUCCESS, message.message);
      } else {
        callback(SendStatus.FAIL, message.message);
      }
    });
    return _channel.invokeMethod(_NativeMethod.SEND_CALLBACK);
  }

  ///
  /// native call flutter method processing
  ///
  Future<dynamic> _nativeHandle(MethodCall call) async {
    String method = call.method;

    switch (method) {
      case _NativeMethod.ON_SEND:

        ///Send message callback
        Map<String, dynamic> params = Map.from(call.arguments);
        _sendController
            .add(_OnSendMessageData(params["status"], params["msg"]));
        break;
      case _NativeMethod.ON_MESSAGE:

        ///Receive new news
        _messageController
            .add(new _OnMessageData(_MessageType.P2P, call.arguments));
        break;
      case _NativeMethod.ON_BROAD_CAST:

        ///Received a new broadcast message callback
        _messageController
            .add(new _OnMessageData(_MessageType.BROADCAST, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_OPENED:

        ///Connection open callback
        _connectionController
            .add(_OnConnectionData(_Connection.OPEN, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_ERROR:

        ///Connection error callback
        _connectionController
            .add(_OnConnectionData(_Connection.ERROR, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_CLOSED:

        ///Connection disconnect callback
        _connectionController
            .add(_OnConnectionData(_Connection.CLOSED, call.arguments));
        break;
    }
    return Future.value("");
  }

  ///
  ///Turn off the streamcontroller object
  ///
  Future _closedStreamControllers() async {
    if (_connectionController != null) {
      _connectionController.close();
      _connectionController = null;
    }
    if (_messageController != null) {
      _messageController.close();
      _messageController = null;
    }
    if (_sendController != null) {
      _sendController.close();
      _sendController = null;
    }
  }
}

///
/// Connection method
///
enum Schema { WS, HTTP }

///
/// Connection callback method
///
enum _Connection { OPEN, ERROR, CLOSED }

///
/// Accepted message type
///
enum _MessageType { P2P, BROADCAST }

///
/// Message sent to status, success 1 failed 0
///
enum SendStatus { FAIL, SUCCESS }

///
/// Connection callback data
///
class _OnConnectionData {
  _Connection call;
  dynamic state;

  _OnConnectionData(this.call, this.state);
}

///
/// Message callback data
///
class _OnMessageData {
  _MessageType type;
  dynamic message;

  _OnMessageData(this.type, this.message);
}

///
/// Send message callback data
///
class _OnSendMessageData {
  int status;
  dynamic message;

  _OnSendMessageData(this.status, this.message);
}

///
/// Define the methods to call, including native and flutter
///
class _NativeMethod {
  ///Flutter calls native
  static const String INIT = "init";
  static const String CONNECTION = "connection";
  static const String SUBSCRIBER_P2P = "subscriberP2P";
  static const String SUBSCRIBER_BROADCAST = "subscriberBroadcast";
  static const String MESSAGE_CALLBACK = "setMessageCallback";
  static const String SEND_CALLBACK = "setSendCallback";
  static const String DESTROY = "destroy";
  static const String SEND_MESSAGE = "sendMessage";

  ///Native reverse flutter
  static const String ON_CONNECTION_OPENED = "onConnectionOpen";
  static const String ON_CONNECTION_ERROR = "onConnectionError";
  static const String ON_CONNECTION_CLOSED = "onConnectionClosed";

  static const String ON_MESSAGE = "onMessage";
  static const String ON_BROAD_CAST = "onBroadcastMessage";
  static const String ON_SEND = "onSend";
}
