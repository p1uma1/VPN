#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <arpa/inet.h>

#define TUN_DEVICE "/dev/net/tun"
#define BUFFER_SIZE 2048

int create_tun_device(char *dev) {
    struct ifreq ifr;
    int fd, err;

    if ((fd = open(TUN_DEVICE, O_RDWR)) < 0) {
        perror("Opening /dev/net/tun");
        return fd;
    }

    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI; // Use TUN mode, no extra packet info
    strncpy(ifr.ifr_name, dev, IFNAMSIZ);

    if ((err = ioctl(fd, TUNSETIFF, (void *)&ifr)) < 0) {
        perror("ioctl(TUNSETIFF)");
        close(fd);
        return err;
    }

    printf("✅ TUN device %s created\n", dev);
    return fd;
}

void handle_tun_traffic(int tun_fd) {
    char buffer[BUFFER_SIZE];
    while (1) {
        int nread = read(tun_fd, buffer, BUFFER_SIZE);
        if (nread < 0) {
            perror("Reading from TUN device");
            break;
        }
        printf("📥 Received %d bytes from TUN\n", nread);

        // Process packet (forward to VPN server)
        // In a real VPN, encrypt and send this to the server
    }
}

int main() {
    char tun_name[IFNAMSIZ] = "tun0";
    int tun_fd = create_tun_device(tun_name);

    if (tun_fd < 0) {
        fprintf(stderr, "Failed to create TUN device\n");
        return 1;
    }

    system("ip link set tun0 up");  // Bring up TUN device
    system("ip addr add 10.8.0.2/24 dev tun0");  // Assign IP

    handle_tun_traffic(tun_fd);

    close(tun_fd);
    return 0;
}
