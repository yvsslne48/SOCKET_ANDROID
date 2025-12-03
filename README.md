# SOCKET_ANDROID

SOCKET_ANDROID est une application de messagerie en temps réel conçue pour offrir une communication rapide, légère et multiplateforme. Le projet comprend une application JavaFX Desktop, une application Android, ainsi qu’un serveur Socket assurant la transmission fiable des messages.

Le système prend en charge le texte, les fichiers, l’audio et la vidéo, tout en utilisant une architecture propre basée sur plusieurs design patterns (Observer, Singleton, Factory, Strategy).

🚀 Fonctionnalités principales

Messagerie instantanée via sockets TCP

Envoi de fichiers (documents, images, etc.)

Appels audio / vidéo utilisant WebRTC

Interface JavaFX moderne pour le client Desktop

Application Android connectée au même serveur

Notifications en temps réel

Gestion propre des connexions : ConnectionService, MessageService, FileTransferService

Architecture modulaire et extensible

🏗️ Architecture du projet

Client Desktop (JavaFX)

Gère l’interface utilisateur

Communique avec le serveur via un ConnectionService

Affiche les messages en temps réel

Client Android

Version mobile avec les mêmes fonctionnalités de messagerie

Intégration WebRTC pour les appels

Serveur Socket

Reçoit, traite et redistribue les messages

Gère les connexions multi-clients

Support des messages : texte, fichiers, audio, vidéo

🧩 Tech Stack

Java 17 / JavaFX

Android (Kotlin/Java)

WebRTC

Sockets TCP

MessageBroker interne

Design Patterns :

Observer Pattern

Singleton

Strategy Pattern (types de messages)

Factory Pattern

🎯 Objectif du projet

Créer une solution simple, performante et personnalisable pour apprendre et maîtriser :

La communication réseau low-level (sockets)

La synchronisation multi-clients

Le partage de données en temps réel

L’intégration WebRTC dans des app Java / Android

📦 État du projet

✔️ Messagerie fonctionnelle
✔️ Transfert de fichier
✔️ Gestion des connexions
⚙️ Appels audio / vidéo en cours d’amélioration
