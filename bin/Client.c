#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <ws2tcpip.h>

#pragma comment(lib, "ws2_32.lib")

#define PORT 17070
#define BUFFER_SIZE 1024

//   function to read a line 
int recvLine(SOCKET sock, char *buffer, int maxlen) {
    int i = 0;
    char c;
    int n;
    while (i < maxlen - 1) {
        n = recv(sock, &c, 1, 0);
        if (n <= 0) return n;
         // disconnected
        if (c == '\n') break;
        buffer[i++] = c;
    }
    buffer[i] = '\0';
    return i;
}

int main() {
    WSADATA wsaData;
    SOCKET sock = INVALID_SOCKET;
    struct sockaddr_in server_addr;
    char buffer[BUFFER_SIZE];
    char input1[50], input2[50], op[5], name[50];

    // Initialize Winsock
    if (WSAStartup(MAKEWORD(2,2), &wsaData) != 0) {
        printf("WSAStartup failed.\n");
        return 1;
    }

    // create socket
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        printf("Socket creation failed: %d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    struct addrinfo hints, *res;
memset(&hints, 0, sizeof(hints));
hints.ai_family = AF_INET;
hints.ai_socktype = SOCK_STREAM;

if (getaddrinfo("6.tcp.eu.ngrok.io", NULL, &hints, &res) != 0) {
    printf("Failed to resolve hostname.\n");
    closesocket(sock);
    WSACleanup();
    return 1;
}

    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(PORT);
server_addr.sin_addr = ((struct sockaddr_in *)res->ai_addr)->sin_addr;

    // connect to server
    if (connect(sock, (struct sockaddr*)&server_addr, sizeof(server_addr)) == SOCKET_ERROR) {
        printf("Connection failed: %d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return 1;
    }

    // read "Enter your name:" 
    if (recvLine(sock, buffer, sizeof(buffer)) <= 0) {
        printf("Server disconnected.\n");
        closesocket(sock);
        WSACleanup();
        return 1;
    }
    printf("%s\n", buffer);

    // send client name
    printf("Enter your name: ");
    fgets(name, sizeof(name), stdin);
    name[strcspn(name, "\n")] = 0;
    send(sock, name, (int)strlen(name), 0);
    send(sock, "\n", 1, 0); 

    // read welcome message
    if (recvLine(sock, buffer, sizeof(buffer)) <= 0) {
        printf("Server disconnected.\n");
        closesocket(sock);
        WSACleanup();
        return 1;
    }
    printf("%s\n", buffer);

    //  calculation loop 
    while (1) {
        // read number 1
        printf("Enter number 1 (or EXIT to quit): ");
        fgets(input1, sizeof(input1), stdin);
        input1[strcspn(input1, "\n")] = 0;

        if (_stricmp(input1, "EXIT") == 0) {
            send(sock, "EXIT\n", 5, 0);
            printf("Disconnected from server.\n");
            break;
        }

        // read number 2
        printf("Enter number 2: ");
        fgets(input2, sizeof(input2), stdin);
        input2[strcspn(input2, "\n")] = 0;

        // read operator
        printf("Enter operator (+, -, *, /): ");
        fgets(op, sizeof(op), stdin);
        op[strcspn(op, "\n")] = 0;

        // send calculation to server
        snprintf(buffer, sizeof(buffer), "NUMBER:%s\n", input1);
        send(sock, buffer, (int)strlen(buffer), 0);

        snprintf(buffer, sizeof(buffer), "NUMBER:%s\n", input2);
        send(sock, buffer, (int)strlen(buffer), 0);

        snprintf(buffer, sizeof(buffer), "OPERATOR:%s\n", op);
        send(sock, buffer, (int)strlen(buffer), 0);

        // receive server response
        if (recvLine(sock, buffer, sizeof(buffer)) <= 0) {
            printf("Server disconnected.\n");
            break;
        }

        if (strncmp(buffer, "RESULT:", 7) == 0) {
            printf("Server result = %s\n", buffer + 7);
        } else if (strncmp(buffer, "ERROR:", 6) == 0) {
            printf("Server error = %s\n", buffer + 6);
        } else {
            printf("Invalid response from server: %s\n", buffer);
        }
    }

    closesocket(sock);
    WSACleanup();
    return 0;
}
