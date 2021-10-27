#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

void connection(){
    struct sockaddr_in my_addr;
    my_addr.sin_family = AF_INET;
    my_addr.sin_port = htons(9090);
    my_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    char buffer[1024]   = "";

    struct sockaddr_in client_addr;
    int listenfd = socket(AF_INET,SOCK_STREAM,0);
    bind(listenfd,(struct sockaddr*)&my_addr,sizeof(my_addr));
    listen(listenfd, 128);
    socklen_t cliaddr_len = sizeof(client_addr);
    int clientfd = accept(listenfd, (struct sockaddr*)&client_addr, &cliaddr_len);
    while(1){
        int k = read(clientfd, buffer, sizeof(buffer));
        if (k>0) {
            printf("%s\n",buffer);
        }
    }
}

int main(){
    connection();
    return 0;
}