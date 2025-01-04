import 'package:flutter/services.dart';

class MediaInfo {
  final String track;
  final String artist;
  final String thumbnailUrl;
  final bool isPlaying;

  MediaInfo({
    required this.track,
    required this.artist,
    required this.thumbnailUrl,
    required this.isPlaying,
  });
}

class MediaHandler {
  static const platform = MethodChannel('com.example.mediaWidget/media');

  static Future<MediaInfo> getCurrentMediaInfo() async {
    try {
      final result = await platform.invokeMethod('getMediaInfo');
      return MediaInfo(
        track: result['track'] ?? 'No track playing',
        artist: result['artist'] ?? 'Unknown artist',
        thumbnailUrl: result['thumbnailUrl'] ?? '',
        isPlaying: result['isPlaying'] ?? false,
      );
    } on PlatformException catch (e) {
      print("Failed to get media info: ${e.message}");
      return MediaInfo(
        track: 'Error getting track info',
        artist: 'Unknown artist',
        thumbnailUrl: '',
        isPlaying: false,
      );
    }
  }

  static Future<void> handleAction(String action) async {
    try {
      await platform.invokeMethod('mediaAction', {'action': action});
    } on PlatformException catch (e) {
      print("Failed to perform media action: ${e.message}");
    }
  }

  static Future<void> togglePlayPause() async {
    await handleAction('playPause');
  }

  static Future<void> nextTrack() async {
    await handleAction('next');
  }

  static Future<void> previousTrack() async {
    await handleAction('previous');
  }
}
