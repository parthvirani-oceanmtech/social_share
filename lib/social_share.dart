import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

class SocialShare {
  static const MethodChannel _channel = const MethodChannel('social_share');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String?> shareInstagramStory(
    String imagePath, {
    String? backgroundTopColor,
    String? backgroundBottomColor,
    String? attributionURL,
    String? backgroundImagePath,
  }) async {
    Map<String, dynamic> args;
    if (Platform.isIOS) {
      if (backgroundImagePath == null) {
        args = <String, dynamic>{
          "stickerImage": imagePath,
          "backgroundTopColor": backgroundTopColor,
          "backgroundBottomColor": backgroundBottomColor,
          "attributionURL": attributionURL
        };
      } else {
        args = <String, dynamic>{
          "stickerImage": imagePath,
          "backgroundImage": backgroundImagePath,
          "backgroundTopColor": backgroundTopColor,
          "backgroundBottomColor": backgroundBottomColor,
          "attributionURL": attributionURL
        };
      }
    } else {
      final tempDir = await getTemporaryDirectory();

      File file = File(imagePath);
      Uint8List bytes = file.readAsBytesSync();
      var stickerData = bytes.buffer.asUint8List();
      String stickerAssetName = 'stickerAsset.png';
      final Uint8List stickerAssetAsList = stickerData;
      final stickerAssetPath = '${tempDir.path}/$stickerAssetName';
      file = await File(stickerAssetPath).create();
      file.writeAsBytesSync(stickerAssetAsList);

      String? backgroundAssetName;
      if (backgroundImagePath != null) {
        File backgroundImage = File(backgroundImagePath);
        Uint8List backgroundImageData = backgroundImage.readAsBytesSync();
        backgroundAssetName = 'backgroundAsset.jpg';
        final Uint8List backgroundAssetAsList = backgroundImageData;
        final backgroundAssetPath = '${tempDir.path}/$backgroundAssetName';
        File backFile = await File(backgroundAssetPath).create();
        backFile.writeAsBytesSync(backgroundAssetAsList);
      }

      args = <String, dynamic>{
        "stickerImage": stickerAssetName,
        "backgroundImage": backgroundAssetName,
        "backgroundTopColor": backgroundTopColor,
        "backgroundBottomColor": backgroundBottomColor,
        "attributionURL": attributionURL,
      };
    }
    final String? response = await _channel.invokeMethod(
      'shareInstagramStory',
      args,
    );
    return response;
  }

  static Future<String?> shareFacebookStory(
      String imagePath, String backgroundTopColor, String backgroundBottomColor, String attributionURL,
      {String? appId}) async {
    Map<String, dynamic> args;
    if (Platform.isIOS) {
      args = <String, dynamic>{
        "stickerImage": imagePath,
        "backgroundTopColor": backgroundTopColor,
        "backgroundBottomColor": backgroundBottomColor,
        "attributionURL": attributionURL,
      };
    } else {
      File file = File(imagePath);
      Uint8List bytes = file.readAsBytesSync();
      var stickerdata = bytes.buffer.asUint8List();
      final tempDir = await getTemporaryDirectory();
      String stickerAssetName = 'stickerAsset.png';
      final Uint8List stickerAssetAsList = stickerdata;
      final stickerAssetPath = '${tempDir.path}/$stickerAssetName';
      file = await File(stickerAssetPath).create();
      file.writeAsBytesSync(stickerAssetAsList);
      args = <String, dynamic>{
        "stickerImage": stickerAssetName,
        "backgroundTopColor": backgroundTopColor,
        "backgroundBottomColor": backgroundBottomColor,
        "attributionURL": attributionURL,
        "appId": appId
      };
    }
    final String? response = await _channel.invokeMethod('shareFacebookStory', args);
    return response;
  }

  static Future<String?> shareTwitter(String captionText, {List<String>? hashtags, String? url, String? trailingText}) async {
    Map<String, dynamic> args;
    String modifiedUrl;
    if (Platform.isAndroid) {
      modifiedUrl = Uri.parse(url ?? '').toString().replaceAll('#', "%23");
    } else {
      modifiedUrl = Uri.parse(url ?? '').toString();
    }
    if (hashtags != null && hashtags.isNotEmpty) {
      String tags = "";
      hashtags.forEach((f) {
        tags += ("%23" + f.toString() + " ").toString();
      });
      args = <String, dynamic>{"captionText": captionText + "\n" + tags.toString(), "url": modifiedUrl, "trailingText": trailingText ?? ''};
    } else {
      args = <String, dynamic>{"captionText": captionText + " ", "url": modifiedUrl, "trailingText": trailingText ?? ''};
    }
    final String? version = await _channel.invokeMethod('shareTwitter', args);
    return version;
  }

  static Future<String?> shareSms(String message, {String? url, String? trailingText}) async {
    Map<String, dynamic>? args;
    if (Platform.isIOS) {
      if (url == null) {
        args = <String, dynamic>{
          "message": message,
        };
      } else {
        args = <String, dynamic>{"message": message + " ", "urlLink": Uri.parse(url).toString(), "trailingText": trailingText};
      }
    } else if (Platform.isAndroid) {
      args = <String, dynamic>{
        "message": message + (url ?? '') + (trailingText ?? ''),
      };
    }
    final String? version = await _channel.invokeMethod('shareSms', args);
    return version;
  }

  static Future<bool?> copyToClipboard(content) async {
    final Map<String, String> args = <String, String>{"content": content.toString()};
    final bool? response = await _channel.invokeMethod('copyToClipboard', args);
    return response;
  }

  static Future<bool?> shareOptions(String contentText, {String? imagePath}) async {
    Map<String, dynamic> args;
    if (Platform.isIOS) {
      args = <String, dynamic>{"image": imagePath, "content": contentText};
    } else {
      if (imagePath != null) {
        File file = File(imagePath);
        Uint8List bytes = file.readAsBytesSync();
        var imagedata = bytes.buffer.asUint8List();
        final tempDir = await getTemporaryDirectory();
        String imageName = 'stickerAsset.png';
        final Uint8List imageAsList = imagedata;
        final imageDataPath = '${tempDir.path}/$imageName';
        file = await File(imageDataPath).create();
        file.writeAsBytesSync(imageAsList);
        args = <String, dynamic>{"image": imageName, "content": contentText};
      } else {
        args = <String, dynamic>{"image": imagePath, "content": contentText};
      }
    }
    final bool? version = await _channel.invokeMethod('shareOptions', args);
    return version;
  }

  static Future<String?> shareWhatsapp(String content) async {
    final Map<String, dynamic> args = <String, dynamic>{"content": content};
    final String? version = await _channel.invokeMethod('shareWhatsapp', args);
    return version;
  }

  static Future<Map?> checkInstalledAppsForShare() async {
    final Map? apps = await _channel.invokeMethod('checkInstalledApps');
    return apps;
  }

  static Future<String?> shareTelegram(String content) async {
    final Map<String, dynamic> args = <String, dynamic>{"content": content};
    final String? version = await _channel.invokeMethod('shareTelegram', args);
    return version;
  }

// static Future<String> shareSlack() async {
//   final String version = await _channel.invokeMethod('shareSlack');
//   return version;
// }

  //Media Social Share Options

  static Future shareOnGallery({required Uint8List imageBytes, required String name}) async {
    assert(imageBytes != null);
    final result = await _channel.invokeMethod('shareOnGallery', <String, dynamic>{'imageBytes': imageBytes});
    return result;
  }

  static Future<Map<dynamic, dynamic>?> getFacebookUser() async {
    Map<dynamic, dynamic>? result;
    try {
      result = await _channel.invokeMethod('getFacebookUser');
    } catch (e) {
      return null;
    }
    return result;
  }



  static Future<String?> shareOnFeedFacebook({
    required String url,
    required String message,
    required String accessToken,
    required int time,
    required String facebookId,
  }) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('message', () => message);
    arguments.putIfAbsent('accessToken', () => accessToken);
    arguments.putIfAbsent('time', () => time);
    arguments.putIfAbsent('facebookId', () => facebookId);
    try {
      return await _channel.invokeMethod('shareOnFeedFacebook', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareStoryOnFacebook({required String url, required String facebookId}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('facebookId', () => facebookId);
    try {
      return await _channel.invokeMethod('shareStoryOnFacebook', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareStoryOnInstagram({required String url}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    try {
      return await _channel.invokeMethod('shareStoryOnInstagram', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareOnFeedInstagram({required String url, required String message}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('message', () => message);
    try {
      return await _channel.invokeMethod('shareOnFeedInstagram', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareOnWhatsApp({required String url, required String message}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('message', () => message);
    try {
      return await _channel.invokeMethod('shareOnWhatsApp', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareOnWhatsAppBusiness({required String url, required String message}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('message', () => message);
    try {
      return await _channel.invokeMethod('shareOnWhatsAppBusiness', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareOnNative({required String url, required String message}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('url', () => url);
    arguments.putIfAbsent('message', () => message);
    try {
      return await _channel.invokeMethod('shareOnNative', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareContent({required String content}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('content', () => content);
    try {
      return await _channel.invokeMethod('shareContent', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> openAppOnStore({required String appUrl}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('appUrl', () => appUrl);
    dynamic result;
    try {
      result = await _channel.invokeMethod('openAppOnStore', arguments);
    } catch (e) {
      return (e as PlatformException).code;
    }
    return result;
  }

  static Future<String?> shareLinkOnWhatsApp({required String link}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('link', () => link);
    try {
      return await _channel.invokeMethod('shareLinkOnWhatsApp', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<String?> shareLinkOnWhatsAppBusiness({required String link}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('link', () => link);
    try {
      return await _channel.invokeMethod('shareLinkOnWhatsAppBusiness', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }

  static Future<bool?> checkPermissionToPublish() async {
    dynamic result;
    try {
      result = await _channel.invokeMethod('checkPermissionToPublish');
    } catch (e) {
      return false;
    }
    return result;
  }

  static Future<bool?> shareLinkOnFacebook({required String link}) async {
    final Map<String, dynamic> arguments = Map<String, dynamic>();
    arguments.putIfAbsent('link', () => link);
    try {
      return await _channel.invokeMethod('shareLinkOnFacebook', arguments);
    } on PlatformException catch (e) {
      throw e;
    }
  }
}
