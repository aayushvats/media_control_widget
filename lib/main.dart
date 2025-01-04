import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'media_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';


void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await HomeWidget.registerBackgroundCallback(backgroundCallback);
  runApp(const MyApp());
}

// Called when the home widget is tapped
@pragma('vm:entry-point')
Future<void> backgroundCallback(Uri? uri) async {
  if (uri?.host == 'media_action') {
    final action = uri?.queryParameters['action'];
    await MediaHandler.handleAction(action ?? '');
  }
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Media Control Widget',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  Uint8List currentThumbnail = Uint8List(0);
  String currentTrack = 'No track playing';
  String currentArtist = 'Unknown artist';
  String thumbnailUrl = '';
  bool isPlaying = false;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _initPlatformState();
    _startPeriodicUpdate();
  }

  Future<void> _initPlatformState() async {
    await HomeWidget.setAppGroupId('group.com.example.mediaWidget');
    _updateWidgetInfo();
  }

  void _startPeriodicUpdate() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _updateWidgetInfo();
    });
  }


  Future<void> _updateWidgetInfo() async {
    final mediaInfo = await MediaHandler.getCurrentMediaInfo();

    // Check if data has changed
    if (currentTrack != mediaInfo.track ||
        currentArtist != mediaInfo.artist ||
        thumbnailUrl != mediaInfo.thumbnailUrl ||
        isPlaying != mediaInfo.isPlaying) {
      setState(() {
        currentTrack = mediaInfo.track;
        currentArtist = mediaInfo.artist;
        thumbnailUrl = mediaInfo.thumbnailUrl;
        currentThumbnail =
            base64Decode(thumbnailUrl.replaceAll('\n', '').replaceAll(' ', ''));
        isPlaying = mediaInfo.isPlaying;
      });

      // Update the home widget only if there's a change
      await HomeWidget.saveWidgetData('track', currentTrack);
      await HomeWidget.saveWidgetData('artist', currentArtist);
      await HomeWidget.saveWidgetData('thumbnail', thumbnailUrl);
      await HomeWidget.saveWidgetData('isPlaying', isPlaying);
      await HomeWidget.updateWidget(
        name: 'MediaControlWidgetProvider',
        androidName: 'MediaControlWidgetProvider',
      );

      // Save data to SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('track', currentTrack);
      await prefs.setString('artist', currentArtist);
      await prefs.setString('thumbnail', thumbnailUrl);
      await prefs.setBool('isPlaying', isPlaying);
    }
  }


  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Media Control'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (thumbnailUrl.isNotEmpty)
              Image.memory(
                currentThumbnail,
                width: 200,
                height: 200,
                fit: BoxFit.cover,
              ),
            const SizedBox(height: 20),
            Text(
              currentTrack,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            Text(
              currentArtist,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  icon: const Icon(Icons.skip_previous),
                  onPressed: () => MediaHandler.previousTrack(),
                ),
                IconButton(
                  icon: Icon(isPlaying ? Icons.pause : Icons.play_arrow),
                  onPressed: () => MediaHandler.togglePlayPause(),
                ),
                IconButton(
                  icon: const Icon(Icons.skip_next),
                  onPressed: () => MediaHandler.nextTrack(),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
