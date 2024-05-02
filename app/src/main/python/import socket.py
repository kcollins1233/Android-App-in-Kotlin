import socket
import struct

def receive_data():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('192.168.0.194', 8080))
    server_socket.listen(1)
    client_socket, addr = server_socket.accept()
    
    # data = client_socket.recv(4)
    # variable = struct.unpack('!i', data)[0]
    
    
    
    client_socket.close()
    server_socket.close()
    
receive_data()
    