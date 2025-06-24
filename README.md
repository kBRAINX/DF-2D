# Solveur d'Équations aux Dérivées Partielles par Différences Finies 2D

## 📋 Table des Matières
- [Description du Projet](#-description-du-projet)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Fonctionnalités](#-fonctionnalités)
- [Algorithmes Implémentés](#-algorithmes-implémentés)
- [Guide d'Utilisation](#-guide-dutilisation)
- [Structure du Code](#-structure-du-code)
- [Exemples et Résultats](#-exemples-et-résultats)
- [Personnalisation](#-personnalisation)

## 🚀 Description du Projet

Ce projet implémente un solveur numérique pour l'équation de Poisson 2D avec conditions aux limites de Dirichlet homogènes :

```math
-ΔU = f  sur le domaine Ω = [0,1] × [0,1]
U(x, y) = g   sur le bord ∂Ω condition de Dirichlet
```

L'objectif est de résoudre numériquement cette équation aux dérivées partielles (EDP) en utilisant la méthode des différences finies, avec plusieurs algorithmes d'itération pour la résolution du système linéaire résultant.

## 🛠️ Prérequis

- Java JDK 11 ou supérieur
- Git (pour cloner le dépôt)
- (Optionnel) Un IDE Java (Eclipse, IntelliJ IDEA, etc.)

## 📥 Installation

1. **Cloner le dépôt** :
   ```bash
   git clone https://github.com/kBRAINX/DF-2D.git
   cd DF-2D
   ```

2. **Compiler le projet** :
   ```bash
   # Compilation standard
   javac src/*.java -d out/
   ```

3. **Exécuter le programme** :
   ```bash
   # Mode graphique (par défaut)
   java -cp out/ Main
   
   # Mode ligne de commande avec paramètres
   java -cp out/ Main --N 50 --methode SOR --omega 1.8 --test CAS1
   ```

## ✨ Fonctionnalités

- **Résolution numérique** de l'équation de Poisson 2D
- **Trois méthodes itératives** :
  - Gauss-Seidel classique
  - Gauss-Seidel avec sur-relaxation (SOR)
  - Gauss-Seidel parallélisé
- **Interface graphique interactive**
- **Visualisation 2D** avec échelle de couleurs et courbes de niveau
- **Analyse de convergence** et calcul d'erreur
- **Plusieurs cas de test** avec solutions analytiques connues

## 🧮 Algorithmes Implémentés

### 1. Méthode de Gauss-Seidel Classique

**Principe** :
- Méthode itérative qui met à jour séquentiellement chaque point du maillage
- Utilise les valeurs les plus récentes des points voisins
- Convergence garantie pour les matrices à diagonale strictement dominante

**Équation d'itération** :
```
U[i,j] = (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] + h²*f[i,j]) / 4
```

**Paramètres** :
- `N` : Nombre de points intérieurs (maillage N×N)
- `tolerance` : Critère d'arrêt pour la convergence (défaut: 1e-6)
- `maxIterations` : Nombre maximal d'itérations (défaut: 1000)

### 2. Gauss-Seidel avec SOR (Successive Over-Relaxation)

**Principe** :
- Améliore la convergence de Gauss-Seidel par sur-relaxation
- Introduit un paramètre de relaxation ω
- Le choix optimal de ω accélère considérablement la convergence

**Équation d'itération** :
```
U_new = (1-ω)*U_old + ω*(U_gauss_seidel)
```

**Paramètres** :
- `omega` : Paramètre de relaxation (1 < ω < 2)
  - ω < 1 : sous-relaxation
  - ω = 1 : équivalent à Gauss-Seidel classique
  - 1 < ω < 2 : sur-relaxation (accélération)

### 3. Gauss-Seidel Parallélisé

**Principe** :
- Implémentation parallèle utilisant plusieurs threads
- Découpe le domaine en sous-domaines traités en parallèle
- Utilise des verrous pour la synchronisation des frontières

**Paramètres** :
- `nbThreads` : Nombre de threads à utiliser (défaut: nombre de cœurs disponibles)

## 🖥️ Guide d'Utilisation

### Interface Graphique

L'interface graphique permet de contrôler tous les aspects de la simulation :

1. **Sélection du cas de test**
   - Choisissez parmi les cas prédéfinis
   - Visualisez la fonction source f(x,y) et la solution exacte (si disponible)

2. **Configuration du maillage**
   - Définissez la finesse du maillage (N×N points)
   - Visualisez le maillage généré

3. **Choix de la méthode**
   - Sélectionnez la méthode de résolution
   - Ajustez les paramètres spécifiques (ω pour SOR, nombre de threads pour la version parallèle)

4. **Paramètres de convergence**
   - Tolérance (critère d'arrêt)
   - Nombre maximal d'itérations
   - Fréquence de mise à jour de l'affichage

5. **Exécution**
   - Bouton "Exécuter" pour lancer la simulation
   - Barre de progression et statistiques en temps réel
   - Possibilité d'arrêt prématuré

6. **Visualisation des résultats**
   - Affichage 2D avec échelle de couleurs
   - Option pour afficher les courbes de niveau
   - Comparaison avec la solution exacte (si disponible)
   - Calcul et affichage de l'erreur

### Ligne de commande

Pour des simulations en lot ou automatisées, utilisez la ligne de commande :

```bash
java -cp out/ Main [options]
```

**Options disponibles** :
- `--N <valeur>` : Nombre de points intérieurs (défaut: 10)
- `--methode <CLASSIQUE|SOR|PARALLELE>` : Méthode de résolution (défaut: CLASSIQUE)
- `--omega <valeur>` : Paramètre de relaxation ω pour SOR (défaut: 1.5)
- `--test <CAS1|CAS2|...>` : Cas de test à utiliser (défaut: CAS1)
- `--tolerance <valeur>` : Tolérance pour la convergence (défaut: 1e-6)
- `--maxIter <valeur>` : Nombre maximal d'itérations (défaut: 1000)
- `--threads <n>` : Nombre de threads pour la version parallèle (défaut: nb cœurs)
- `--nogui` : Désactive l'interface graphique (pour du traitement par lots)

## 🏗️ Structure du Code

```
src/
├── Main.java                 # Point d'entrée du programme
├── DifferencesFinis2DInterface.java  # Interface graphique principale
├── Maillage.java             # Gestion du maillage et conditions aux limites
├── SolveurGaussSeidel.java   # Implémentation des algorithmes de résolution
├── VisualiseurGraphique.java # Affichage des résultats 2D
└── AnalyseurErreurs.java     # Calcul des erreurs et analyse de convergence
```

### Détails des classes principales

#### `Maillage.java`
- Gère la discrétisation du domaine [0,1]×[0,1]
- Implémente les conditions aux limites de Dirichlet
- Fournit les fonctions de test et solutions exactes
- Gère la conversion entre indices 2D et indices linéaires

#### `SolveurGaussSeidel.java`
- Implémente les trois variantes de l'algorithme
- Gère la parallélisation avec des threads
- Calcule les métriques de convergence

#### `VisualiseurGraphique.java`
- Affiche les solutions en 2D avec échelle de couleurs
- Gère les courbes de niveau
- Permet la navigation (zoom, déplacement)

## 📊 Exemples et Résultats

### Cas de Test Disponibles

1. **CAS1** : Solution lisse
   - `f(x,y) = -2π²sin(πx)sin(πy)`
   - Solution exacte : `U(x,y) = sin(πx)sin(πy)`

2. **CAS2** : Solution plus oscillante
   - `f(x,y) = -8π²sin(2πx)sin(2πy)`
   - Solution exacte : `U(x,y) = sin(2πx)sin(2πy)`

3. **CAS3** : Terme source constant
   - `f(x,y) = 1`
   - Solution exacte : Non disponible

4. **CAS4** : Terme source quadratique
   - `f(x,y) = x² + y²`
   - Solution exacte : Non disponible

5. **CAS5** : Solution polynomiale
   - `f(x,y) = -2(x² + y² - x - y)`
   - Solution exacte : `U(x,y) = x(1-x)y(1-y)`

### Analyse des Résultats

L'interface permet de :
- Visualiser la solution numérique calculée
- Superposer la solution exacte (lorsqu'elle est disponible)
- Calculer et afficher l'erreur absolue et relative
- Tracer l'historique de la norme du résidu
- Calculer l'ordre numérique de convergence

## 🛠️ Personnalisation

### Ajouter un nouveau cas de test

1. Modifiez l'énumération `CasTest` dans `Maillage.java` :
   ```java
   public enum CasTest {
       // ... autres cas ...
       MON_CAS("Description du cas", "Solution exacte");
       
       private final String description;
       private final String solutionExacte;
       // ...
   }
   ```

2. Implémentez les méthodes pour le terme source et la solution exacte :
   ```java
   private double calculerTermeSource(int i, int j, CasTest cas) {
       // ...
       case MON_CAS:
           double x = i * h;
           double y = j * h;
           return /* expression de f(x,y) */;
       // ...
   }
   
   private double calculerSolutionExacte(int i, int j, CasTest cas) {
       // ...
       case MON_CAS:
           double x = i * h;
           double y = j * h;
           return /* expression de U_exacte(x,y) */;
       // ...
   }
   ```

### Modifier les paramètres par défaut

Les paramètres par défaut peuvent être modifiés dans les constantes des classes respectives :
- `SolveurGaussSeidel.TOLERANCE_DEFAULT`
- `SolveurGaussSeidel.MAX_ITERATIONS_DEFAULT`
- `DifferencesFinis2DInterface.DEFAULT_N`
- etc.

## 📄 Licence

Ce projet est sous licence [MIT](LICENSE).
