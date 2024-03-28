import socket

def send_image():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    port = 12345
    s.connect(("192.168.0.199", port))
    s.listen(4)
    print("Listening on port: ", port)
    
    # Load the image
    image = Image.open("received_image.bmp")
    
    # Convert the image to a byte array
    byte_data = io.BytesIO()
    image.save(byte_data, format="BMP")
    byte_data = byte_data.getvalue()
    
    # Send the size of the byte array
    length = struct.pack('!i', len(byte_data))
    s.send(length)
    
    # Send the byte array
    s.send(byte_data)
    
    # Close the connection
    s.close()
    
if __name__ == "__main__":
    send_image()