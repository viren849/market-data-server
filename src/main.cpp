#include "server.h"

int main() {
    TCPServer server(9000);
    server.start();
    return 0;
}
