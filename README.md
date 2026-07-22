# l2j_fiskpay

FiskPay brings cryptocurrency payment integration to Lineage 2 Java emulators, enabling seamless in-game transactions.


## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Requirements](#requirements)
- [Instructions](#instructions)
- [Disclaimer](#disclaimer)
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

The in-game reward item ID is not configured in the database. It is configured in your Login Server `Blockchain.ini` file using `RewardId`.

Set `ConversionRate` in `Blockchain.ini` to define how many in-game reward items equal one blockchain service unit. Accepted values are `1`, `10`, `100`, and `1000`.

### 4. Register your L2J emulator to the service

Register your server at:

[https://l2.fiskpay.com/](https://l2.fiskpay.com/)

To register, connect your Web3 wallet and enter your desired Login Server password and remote IPv4 address. Registration creates an encrypted signer file locally in your browser. Download the file, keep its filename as `signer`, and place it in your Login Server `config` folder.

The signer file is encrypted with the same password you enter during registration. Keep this file private. It is required by the Login Server to sign withdrawal transactions.

### 5. Enable blockchain support

Open your Login Server `Blockchain.ini` config file, enable the integration, and add your settings:

```ini
Enable = True
Symbol = USDT0
RewardId = 4037
ConversionRate = 1
Wallet = YOUR_WALLET_ADDRESS_HERE
Password = YOUR_SUPER_SECRET_PASSWORD_HERE
```

`Symbol` and `Wallet` are shared by all Game Servers connected to the Login Server.

`RewardId` must be a numeric item ID. The item must exist in the Game Server and must be stackable.

`ConversionRate` is only for game/UI conversion. The blockchain contract still uses raw service units. Accepted values are `1`, `10`, `100`, and `1000`.

Set `Password` to the same password used during registration. Make sure the downloaded signer file is available at `./config/signer` relative to the Login Server.

### 6. Compile & Launch

Build and launch your L2J server, depending on your project.

### 7. Access your blockchain panel

Manage transactions between your server and the blockchain via the following link:

https://l2.fiskpay.com/YOUR_ETHEREUM_ADDRESS_HERE/

Replace YOUR_ETHEREUM_ADDRESS_HERE with your actual Ethereum wallet address.

## In-game Preview

<div align="center"><img width="828" height="477" alt="FiskPay in-game preview" src="https://github.com/user-attachments/assets/9f449c29-1b1d-473e-9b74-8e5ac455251c" /></div>


## Disclaimer

This patch is provided as an example of how the FiskPay service can be implemented in an L2J emulator.

How the service is installed, configured, modified, operated, and secured is the responsibility of each client or server owner. FiskPay is not responsible for improper use, insecure deployment, incompatible changes, or any damage caused by using this patch incorrectly.


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
