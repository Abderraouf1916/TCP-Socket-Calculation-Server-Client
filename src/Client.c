#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif

#define BUFFER_SIZE 1024

// Checks if operation is binary
int isBinary(const char *op) {
    const char *binaryOps[] = {"ADD","SUB","MUL","DIV","POW","MOD","MAX","MIN","AVERAGE"};
    for(int i=0;i<10;i++)
        if(strcmp(op,binaryOps[i])==0) return 1;
    return 0;
}

// Send message with newline
void sendLine(int sock, const char *msg) {
    char buffer[BUFFER_SIZE];
    snprintf(buffer, sizeof(buffer), "%s\n", msg);
#ifdef _WIN32
    send(sock, buffer, (int)strlen(buffer), 0);
#else
    send(sock, buffer, strlen(buffer), 0);
#endif
}

int recvLine(int sock, char *buf, int size){
    int i = 0;
    char c;
    while(i < size - 1){
        int n = recv(sock, &c, 1, 0);
        if(n <= 0) return n; // disconnected
        if(c == '\n') break;
        buf[i++] = c;
    }
    buf[i] = '\0';
    return i;
}

int main() {
#ifdef _WIN32
    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);
#endif

    char hostname[256] = "localhost"; 
    int port = 5000;                     

    struct sockaddr_in server_addr;
    struct hostent *he;

#ifdef _WIN32
    SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
#else
    int sock = socket(AF_INET, SOCK_STREAM, 0);
#endif
    if(sock < 0){ perror("Socket error"); return 1; }

    he = gethostbyname(hostname);
    if(he == NULL){ perror("gethostbyname error"); return 1; }

    memset(&server_addr,0,sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);
    memcpy(&server_addr.sin_addr, he->h_addr_list[0], he->h_length);

    if(connect(sock,(struct sockaddr*)&server_addr,sizeof(server_addr))<0){
        perror("Connect error"); return 1; 
    }

    char buffer[BUFFER_SIZE];

    if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); goto end; }
    printf("%s\n", buffer);

    char name[50];
    fgets(name, sizeof(name), stdin);
    name[strcspn(name,"\n")] = 0;
    sendLine(sock, name);

    if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); goto end; }
    printf("%s\n", buffer);

    while(1){
        if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); break; }
        printf("%s\n> ", buffer);

        char op[20];
        fgets(op, sizeof(op), stdin);
        op[strcspn(op,"\n")] = 0;
        if(strcmp(op,"EXIT")==0){
            sendLine(sock,"EXIT");
            printf("Disconnected from server.\n");
            break;
        }
        sendLine(sock, op);

        // arg1
        if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); break; }
        printf("%s\n> ", buffer);
        char arg1[50];
        fgets(arg1,sizeof(arg1),stdin);
        arg1[strcspn(arg1,"\n")]=0;
        sendLine(sock,arg1);

        // arg2 
        if(isBinary(op)){
            if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); break; }
            printf("%s\n> ", buffer);
            char arg2[50];
            fgets(arg2,sizeof(arg2),stdin);
            arg2[strcspn(arg2,"\n")]=0;
            sendLine(sock,arg2);
        }

        //  result
        if(recvLine(sock, buffer, BUFFER_SIZE)<=0){ printf("Server disconnected.\n"); break; }
        printf("%s\n", buffer);
    }

end:
#ifdef _WIN32
    closesocket(sock);
    WSACleanup();
#else
    close(sock);
#endif
    return 0;
}
