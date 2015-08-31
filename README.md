# BabyMonitor
This is an Android project of baby monitor.

The baby app can distinguish baby crying in noisy environment and send baby video to parents. When it detect baby crying, it will play a lullaby and start a timer. After a while, if baby is still crying, it will call parents. The parent app can receive warning from baby app and see video from it. If parent think there is nothing wrong, he or she can turn off the lullaby remotely.

The baby app use FFT to detect baby crying.

The baby app is tested on Rockchip PX2.

Reference:

[FFT package](http://www.netlib.org/fftpack/)

[Mjpegview](https://bitbucket.org/neuralassembly/simplemjpegview)

[Socket communication](http://codeoncloud.blogspot.ca/2014/06/android-tcpip-client-server-socket.html)

