# l2j_fiskpay

L2J FiskPay brings cryptocurrency payment integration to L2J servers, enabling seamless in-game transactions.


## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Requirements](#requirements)
- [Instructions](#instructions)
- [License](#license)


## Introduction

L2J FiskPay is a customized server integration project for L2J Mobius that adds blockchain payment functionality via FiskPay. This project enables in-game transactions (deposits, withdrawals) linked to Ethereum wallet addresses, allowing players to interact with blockchain-based services seamlessly, using the Polygon Network.


## Features

- **Blockchain Integration**: Connects L2J Mobius with the FiskPay blockchain service for handling deposits and withdrawals.
- **Account Linking**: Allows players to link/unlink their Ethereum wallet addresses with their in-game accounts.
- **Game Server Communication**: Uses asynchronous communication to handle requests between the Login Server and Game Servers.
- **Secure Transactions**: Implements security checks such as wallet ownership verification and anti-exploit measures..


## Requirements

- L2J Mobius Server
- FiskPay blockchain service credentials
- Basic knowledge of how to apply patches using Java
- Web3 Wallet (i.e. MetaMask)


## Instructions

### 1. Copy Java Files

Copy all `.java` files from the `l2jmobius` folder in this repository to your local L2JMobius project.

### 2. Apply Patch Files

Apply all `.java.diff` patch files from the `l2jmobius` folders in this repository to your L2JMobius project.

### 3. Update your MySQL Database

Update your Login Server database using the files located in this repository `l2jmobius/dist/db_installer/sql/` folder.

### 3. Register your L2J server to the service

Register your L2J server by obtaining service credentials at:

[https://l2.fiskpay.com/admin/USER_ETHEREUM_WALLET_ADDRESS_HERE/](https://l2.fiskpay.com/admin/USER_ETHEREUM_WALLET_ADDRESS_HERE/)

Replace `USER_ETHEREUM_WALLET_ADDRESS_HERE` with your actual Ethereum wallet address. To register, use your desired password and your Login Server remote IPv4 address.

### 4. Edit FiskPayLoginClient.java and GSMethods.java files

Open `FiskPayLoginClient.java` and add your credentials to the appropriate constants. Then, if needed, open `GSMethods.java` and replace the reward item id.

### 5. Complile your local L2J Mobius project

Compile your local L2J Mobius project by following the instuctions given at:
[https://l2jmobius.org/forum/index.php?topic=3231.0](https://l2jmobius.org/forum/index.php?topic=3231.0)

### 6. Access your blockchain panel

Manage transactions between your server and the blockchain via the following link:

[https://l2.fiskpay.com/USER_ETHEREUM_WALLET_ADDRESS_HERE/](https://l2.fiskpay.com/USER_ETHEREUM_WALLET_ADDRESS_HERE/)

Replace USER_ETHEREUM_WALLET_ADDRESS_HERE with your actual Ethereum wallet address. 


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
