## Documentation

### Folder Strucure of windows
- /assets - contains icon for the .exe, qr icons and the generated qr code 
- /crypto - deals with crypotgraphy i.e., key generation, storage, retrieval, qr code generation, challenge signing
- /gui - handles the user interface
- /network - contains the networking logic

### Workflow of the code 

I generate a public key and a private key for authentication, extract the ip address (public) of my laptop. I create a qr code containing these three information. I create a socket that sends this information to the android side. The android side connects this socket via a TCP handshake. Further authentication is done by sharing a challenge by the android. PC signs and android verifies. Connection is now established.

I am using the same socket that was used initially. I have to pass this socket to the caller function and upon successful authentication, two threads are called - sending and receiving. Any blocking activity should be called in a thread so as not to interfere with the UI. 

Similarly, in the PC side, the same socket is used and two threads are called (non-daemon).

The UI in PC is written using ```PyQt5```. The ```listen()``` which is responsible for starting the server in the PC side is called as a daemon. UI closer would lead to closure of any background threads. 

#### COBS

Any data that is being sent from either side is first converted to bytes and then a header is added signifying the data format of the data and length and then encoded using cobs. 

Cobs algorithm is very simple. It removes any ```b'/00'``` and the maximum updated length would be ```N + floor(N / 254) + 1```. Consider a byte buffer that contains all non zero byte followed by a zero byte. When zero byte is encountered, it calculates the length of the byte stream upto the zero byte. Preceeding the byte stream, a byte is added which denotes the length of the non-zero byte stream. Maximum length of the non-zero byte stream could be 254.

### Working for the first time in Kotlin?

I had used Android Studio for developing an android application. When you create a project, like in React, you get a default structure for your application. I shall explain briefly what those folders are. 

The structure of the project will be completely different when you open in a code editor like VS-Code against Android Studio. Work in Android Studio as the gradle building and debugging is much simplier.  

You can see two sections :
- App 
- Gradle Scripts 

#### App 

Ever seen the a popup window asking for permission for storage, location, etc? Apps can't have those permissions unless you provide them. If your code requires such permissions, you should use ```AndroidManifest.xml```. In short, anything you want to run without direct user interaction, you need to mention it here otherwise Android will cry. Any background service that needs to run should have a persistent notification otherwise Android kills the process. If you are using any library to handle certain activities, those library handles it, no need to add in the ```.xml``` file.

kotlin/java folder will contain the logic of your project/modules. 


Right click on the app folder to add tons of stuff. The only use of this feature for me was to add the icon images for the application.

#### Gradle 

Gradle is equivalent to what you do when you use new packages. Instead of manually installing it, you add them in ```libs.versions.toml``` and add the dependency in ```build.gradle.kts```. Use the module version instead of app version for simpler projects. 

#### Running the app in mobile

I found the wired debugging much faster. Switch on the developer mode on your phone. Google this. Then, just connect your pc and phone via USB. Voila! Don't forget to switch on auto-android on the notification bar.  


##  Some lessons learned the hard way

1. First issue that I faced was whether it was possible to connect devices in different network
via NAT traversal. Decided to drop this after failing to understand how udp hole punching would work after d2(android)
know's d1(pc) ip address but not vice versa.

2. I was going with UDP, but TCP is much simpler to implement and had to change most of my work. Decide the architecture and design before you build.

3. Firewall blocked my connection to android and I was wondering what could be the problem.
Changed inbound/outbound rules. Changed network profiles. Still, antivirus caused problem. Atlast turning off antivirus worked.

4. When you write blocking code. You can't close the process using some keyboard interrupt. Atleast, it didn't work for me. Activating venv and calling the main.py was a headache. You can solve this using ```keybindings.json``` and put the whole change directory, environment activation and running the file into this and give it a shorcut. For me it was ```ctrl + shift + ;```

5. When you try to read and write data, these are blocking functions. You need to define conditons for STOPPING. Otherwise, they would expect to receive data and throw error for some cases (depends on your config). I didn't define this STOP condition.

6. The challenge that is sent is a byte string.
but it was hex decoded. Signing it would be erroneous because you are signing the individual
characters instead of the bytes. Example if the challenge received is b'12345678...'
decoding and converting back to byte would result in different bytes(actually /2 of the length)
but signing it directly would be equivalent to treating 1 as a byte, 2 as a byte and so on. So, the format in which the data is sent is crucial.

7. Somehow after sometime, even if i have clearly stated that there should be no timeout,
the port closes from mobile side. Understanding the underlying functonality is essential to create an application.

8. There is this problem while sending the data from phone to laptop, i have to open the application 
on order for the data to pass to the laptop, otherwise it doesn't. Applications can't access clipboard in modern Android versions from background. 