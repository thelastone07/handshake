import socket
from network.port_info import LOCAL_PORT, get_public_ip
from crypto.keygen import sign_challenge
import time
import threading
import pyperclip

def read_initial(soc, buffer=1024)->bytes:
    try:
        while True:
            chunk = soc.recv(buffer)
            if not chunk:
                break
            return chunk
    except Exception as e:
        print("reading intial data failed :",e)
    
text = pyperclip.paste()
text_rec = ""

def send(soc):
    print('i am listening')
    try:
        while True:
            global text, text_rec
            curr = pyperclip.paste()
            should_send = curr != "" and text != curr and curr != text_rec
            if should_send:
                print("printing data to be sent")
                print(curr)
                message = curr.encode()
                print(message)
                soc.sendall(message)
            else:
                time.sleep(0.5)
            text = curr
    except Exception as e:
        print("Error or client disconnected: ", e)
    finally:
        print("Closing connection...")
        soc.close()
                


def listen():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ip = get_public_ip()
    server.bind((f'{ip}', LOCAL_PORT))
    server.listen(1)

    soc, addr = server.accept()

    #receive first message 
    try :
        data = read_initial(soc).decode()
        data = data.rstrip('\n').split('|')[1]
        signature = sign_challenge(bytes.fromhex(data))

        if not isinstance(signature, bytes):
            raise ValueError("sign not bytes")
        if len(signature) != 64:
            raise ValueError("sign not 64 in length")

    except Exception as e:
        print("error while reading initial data :", e)

    #send intial message
    try :
        soc.sendall(signature) 
    except Exception as e:
        print("signature can't send :", e)


    thread = threading.Thread(target=send, args=(soc,))
    thread.start()


