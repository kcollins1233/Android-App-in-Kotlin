import socket

# Create a socket object
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Bind the socket to a specific IP address and port
server_socket.bind(('127.0.0.1', 12345))

# Listen for incoming connections
server_socket.listen()

# Accept a connection from a client
client_socket, client_address = server_socket.accept()

# Receive data from the client
data = client_socket.recv(1024)

# Process the received data
print(f"Received data from client: {data.decode()}")

# Close the connection
client_socket.close()
server_socket.close()
