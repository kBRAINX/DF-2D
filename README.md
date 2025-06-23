# Projet de Résolution Numérique par Différences Finies 2D

## Description du Projet

Ce projet implémente un solveur numérique pour l'équation de Poisson 2D avec conditions aux limites de Dirichlet homogènes :

```
-ΔU = f  sur le domaine Ω = [0,1] × [0,1]
U = 0   sur le bord ∂Ω
```

Le programme offre une interface graphique interactive permettant de :
- Sélectionner différents cas de test
- Choisir la méthode de résolution
- Visualiser les résultats en 2D avec coloration et courbes de niveau
- Analyser la convergence des méthodes itératives

## Prérequis

- Java JDK 11 ou supérieur
- Maven (pour la gestion des dépendances)

## Installation

1. Cloner le dépôt :
   ```bash
   git clone [https://github.com/kBRAINX/DF-2D.git]
   cd DF-2D
   ```

2. Compiler le projet :
   ```bash
   javac src/*.java -d out/
   ```

3. Exécuter le programme :
   ```bash
   java -cp out/ Main
   ```

## Algorithmes Implémentés

### 1. Méthode de Gauss-Seidel Classique

**Principe** : Méthode itérative qui met à jour séquentiellement chaque point du maillage en utilisant les valeurs les plus récentes des points voisins.

**Paramètres** :
- `N` : Nombre de points intérieurs (maillage N×N)
- `tolerance` : Critère d'arrêt pour la convergence (défaut: 1e-6)
- `maxIterations` : Nombre maximal d'itérations (défaut: 1000)

### 2. Gauss-Seidel avec SOR (Successive Over-Relaxation)

**Principe** : Amélioration de la méthode de Gauss-Seidel avec un paramètre de relaxation ω pour accélérer la convergence.

**Paramètres supplémentaires** :
- `omega` : Paramètre de relaxation (1 < ω < 2)

### 3. Gauss-Seidel Parallélisé

**Principe** : Implémentation parallèle de Gauss-Seidel utilisant plusieurs threads pour accélérer les calculs.

**Paramètres supplémentaires** :
- `nbThreads` : Nombre de threads à utiliser (par défaut: nombre de processeurs disponibles)

## Cas de Test

Le programme propose plusieurs cas de test prédéfinis :

1. `f = -2π²sin(πx)sin(πy)` avec solution exacte `U = sin(πx)sin(πy)`
2. `f = -8π²sin(2πx)sin(2πy)` avec solution exacte `U = sin(2πx)sin(2πy)`
3. `f = 1` (constante)
4. `f = x² + y²`
5. `f = -2(x²+y²-x-y)` avec solution exacte `U = x(1-x)y(1-y)`

## Utilisation de l'Interface Graphique

1. **Sélection du cas de test** : Choisissez parmi les différents cas prédéfinis.
2. **Configuration du maillage** : Définissez le nombre de points dans chaque direction.
3. **Choix de la méthode** : Sélectionnez la méthode de résolution souhaitée.
4. **Paramètres avancés** : Ajustez la tolérance, le nombre maximal d'itérations, etc.
5. **Lancement du calcul** : Cliquez sur "Exécuter" pour démarrer la résolution.
6. **Visualisation** : Les résultats s'affichent avec une échelle de couleurs et des courbes de niveau.

## Analyse des Résultats

L'interface permet de :
- Visualiser la solution numérique
- Afficher la solution exacte (si disponible)
- Calculer et afficher l'erreur
- Tracer l'historique de convergence
- Calculer l'ordre numérique de convergence

## Exemple de Ligne de Commande

Pour exécuter avec des paramètres spécifiques :
```bash
java -cp out/ Main --N 50 --methode SOR --omega 1.8 --test CAS1
```

## Structure du Code

- `Main.java` : Point d'entrée du programme
- `DifferencesFinis2DInterface.java` : Interface graphique principale
- `Maillage.java` : Gestion du maillage et des conditions aux limites
- `SolveurGaussSeidel.java` : Implémentation des méthodes de résolution
- `VisualiseurGraphique.java` : Affichage des résultats
- `AnalyseurErreurs.java` : Calcul des erreurs et analyse de convergence

## Aperçu des Résultats

Après exécution, le programme affiche :
- La solution numérique
- La solution exacte (si disponible)
- L'erreur
- L'historique de convergence
- Des statistiques sur les performances

## Personnalisation

Pour ajouter un nouveau cas de test, modifiez l'énumération `CasTest` dans `Maillage.java` et implémentez les méthodes correspondantes pour le terme source et la solution exacte.
