# handshake

A TCP-based data sharing service implemented from scratch using Python and Kotlin. A ```ED25519``` based sign is exchanged for more layered authentication. Data is encoded via ```COBS``` for persistent communication. Find documentation [here](documentation.md).

## Features 
1. Enables clipboard sharing across android and a windows device.
2. No size limit.
3. Automatic files backup from your android to your windows device. (in progress)

## Steps to Install

### Android

- Find the apk in the ```app_android.zip```. Use your app installer to install the application. 

*No virus :)*

### Windows

> Supports only 64-bit architecture

- Find the ```pc_exe.zip```. Unzip it. Run the executable to launch the service. 
- Go to ```Windows Defender Firewall with Advanced Security```.
- Add ```inbound``` and ```outbound``` rules. 
- Click on ```New Rule```-> ```Port```-> ```TCP```->```5005```->```Private```.
- You might need to add the same rules in your antivirus if it is installed.


## How to Use?
- Run the .exe in your windows and click on start listening. 
- Go to your android application and click on connect.
- Click show qr on the dekstop application and scan the qr code on your phone.
- Now, you can start sharing data across clipboard.
- Laptop service stays on indefinitely. The mobile service turns off after a period of 20s of inactivity. 
- In case of such inactivity, just click on past connections and you are connected instantly.
- Android versions 8+ doesn't allow applications to access your clipboard using background service. In order to send clipboard data from your phone to windows, you have to open the application after copying the data. Vice versa is not applicable. 

### Notes
- If there is a warning of missing DLLs, please raise an issue. 
- If you find that adding inbound and outbound rules still doesn't help. Please raise an issue.


I am not adequately familiar with designing services that bypass restrictions imposed by adapters and firewalls. If you have any notes for improvement, I would love to hear it. Please find my contact details in my profile. 



