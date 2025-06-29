from crypto.keygen import create_key

def main():
    pvk, pbk = create_key()
    print("Private Key:", pvk)
    print("Public Key:", pbk)


if __name__ == "__main__":
    main()