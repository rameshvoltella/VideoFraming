README

Android Face Extraction App 


Software Required:
1. Eclipse
2. Android SDK version 5.0.1 


Libraries Required:
1. Android Support Library v7 


Necessary Files:
1. place the video file "1.mp4" in the /sdcard/Download/" folder path on the android device


How to Run the App:
1. Import the "VideoFrame" project folder in Eclipse.
2. Change the Android version to 5.0.1 in project properties, as well as import the Android Support Library v7
3. Compile & Run


Output:
1. In the "Download" folder there should be one output of "1enc.mp4", and in /sdcard/VideoFraming/" folder there should be the blacked out frames.
2. "1.mp4" is the original video file that you will be using to run the tests, which will result in 2 output files: The video frames in jpeg format depending on the amount of frames you want extracted, and the encrypted video file of the original video using AES 256 encryption.
3. Since the video is shot in 1080P 60fps, the frames will be in 1920x1080 resolution. The image size will be relativly around +- 1 MB for each frame 