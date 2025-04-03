# l2j_fiskpay

FiskPay brings cryptocurrency payment integration to Lineage 2 Java emulators, enabling seamless in-game transactions.


## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Requirements](#requirements)
- [Instructions](#instructions)
- [License](#license)


## Introduction

This repository is a customized integration project for Lineage 2 Java emulators, adding blockchain payment functionality through FiskPay. It enables in-game transactions (deposits and withdrawals) linked to Ethereum wallet addresses, allowing players to seamlessly interact with blockchain-based services on the Polygon Network.


## Features

- **Blockchain Integration**: Connects Lineage 2 Java emulators with the FiskPay blockchain service for handling deposits and withdrawals.
- **Account Linking**: Allows players to link/unlink their Ethereum wallet addresses with their in-game accounts.
- **Game Server Communication**: Uses asynchronous communication to handle requests between the Login Server and Game Servers.
- **Secure Transactions**: Implements security checks such as wallet ownership verification and anti-exploit measures.
- **Real-time Transaction Verification**: Ensures instant validation of blockchain transactions, reducing wait times and preventing fraud.
- **Automated Delivery System**: Delivers in-game currency automatically upon successful blockchain transaction confirmation.
- **Support for Multiple Cryptocurrencies**: Enables transactions using various cryptocurrencies, providing flexibility for administrators.
- **User-friendly Payment Interface**: Offers a seamless and intuitive payment experience, simplifying blockchain transactions for users.


## Requirements

- Lineage 2 Java emulator source files
- FiskPay blockchain service credentials
- Basic knowledge of how to apply patches using Java
- Web3 Wallet (i.e. MetaMask)


## Instructions

### 1. Copy Java Files

Depending your Lineage 2 Java emulator, copy all files from the `emulators` directory to your local L2J project.

### 2. Apply Patch Files

Apply all `.java.diff` patch files to your L2J project.

### 3. Update your MySQL Database

Apply the SQL updates to your Login Server database.

### 3. Register your L2J emulator to the service

Register your server at:

[https://l2.fiskpay.com/](https://l2.fiskpay.com/)

To register, use your desired password and your Login Server remote IPv4 address.

### 4. Edit FiskPayLoginClient.java and GSMethods.java files

Open `FiskPayLoginClient.java` and add your credentials to the appropriate constants. Then, if needed, open `GSMethods.java` and replace the reward item id.

### 5. Compile & Launch

Build and launch your L2J server, depending on your project.

### 6. Access your blockchain panel

Manage transactions between your server and the blockchain via the following link:

[https://l2.fiskpay.com/YOUR_ETHEREUM_ADDRESS_HERE/](https://l2.fiskpay.com/YOUR_ETHEREUM_ADDRESS_HERE/)

Replace YOUR_ETHEREUM_ADDRESS_HERE with your actual Ethereum wallet address. 


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
