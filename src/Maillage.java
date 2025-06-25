/**
 * Classe représentant le maillage 2D pour la résolution par différences finies
 * avec conditions aux limites de Dirichlet générales
 *
 * Cette classe gère :
 * - La discrétisation du domaine [0,1]×[0,1]
 * - Les conditions aux limites de Dirichlet générales U(bord) = valeurs spécifiées
 * - Les fonctions de test et solutions exactes
 * - La conversion entre indices 2D et indices linéaires pour les points INTÉRIEURS
 *
 * Le maillage utilise un schéma de différences finies à 5 points sur une grille uniforme.
 * N représente le nombre total de points dans chaque direction (y compris les bords).
 * Les points intérieurs vont de l'indice 1 à N-2.
 * Les points sur les bords sont aux indices 0 et N-1.
 */
public class Maillage {

    // Paramètres du maillage
    private int N_total;              // Nombre TOTAL de points dans chaque direction (bords inclus)
    private int N_interieur;          // Nombre de points intérieurs dans chaque direction (N_total - 2)
    private double h;                 // Pas du maillage h = 1/(N_total-1)
    private double[][] U;             // Solution U(x,y) aux points du maillage
    private double[][] F;             // Terme source f(x,y)
    private double[][] exactSol;      // Solution exacte (si connue)
    private boolean[][] boundary;     // Masque des conditions aux limites

    // Conditions aux limites
    private ConditionsLimites conditionsLimites;

    // Cas de test disponibles (adaptés aux conditions générales)
    public enum CasTest {
        CAS1("f = -2π²sin(πx)sin(πy)", "U_exact = sin(πx)sin(πy)", "homogenes"),
        CAS2("f = -8π²sin(2πx)sin(2πy)", "U_exact = sin(2πx)sin(2πy)", "homogenes"),
        CAS3("f = 1 (constante)", "Solution inconnue", "constantes_variables"),
        CAS4("f = x² + y²", "Solution inconnue", "lineaires"),
        CAS5("f = -2(x²+y²-x-y)", "U_exact = x(1-x)y(1-y)", "homogenes"),
        CAS6("f = 0 (Laplace)", "Solution inconnue", "constantes_unitaires"),
        CAS7("f = 4π²sin(πx)cos(πy)", "Solution dépend des CL", "sinusoidales"),
        CAS8("f = 2", "Solution quadratique", "polynomiales");

        private final String description;
        private final String solutionExacte;
        private final String conditionsRecommandees;

        CasTest(String desc, String sol, String cond) {
            this.description = desc;
            this.solutionExacte = sol;
            this.conditionsRecommandees = cond;
        }

        public String getDescription() { return description; }
        public String getSolutionExacte() { return solutionExacte; }
        public String getConditionsRecommandees() { return conditionsRecommandees; }
    }

    /**
     * Constructeur du maillage
     *
     * @param N_total Nombre total de points dans chaque direction (bords inclus). Doit être >= 3.
     */
    public Maillage(int N_total) {
        if (N_total < 3) {
            throw new IllegalArgumentException("N_total doit être au moins 3 pour avoir des points intérieurs.");
        }
        this.N_total = N_total;
        this.N_interieur = N_total - 2;
        this.h = 1.0 / (this.N_total - 1); // N_total points => N_total-1 intervalles

        // Initialisation des tableaux N_total × N_total
        this.U = new double[N_total][N_total];
        this.F = new double[N_total][N_total];
        this.exactSol = new double[N_total][N_total];
        this.boundary = new boolean[N_total][N_total];

        // Conditions aux limites par défaut (homogènes)
        this.conditionsLimites = new ConditionsLimites();

        // Marquage des points frontière
        initializeBoundary();

        System.out.println("Maillage initialisé : " + N_total + "×" + N_total + " points, h = " + h +
            ", N_interieur = " + N_interieur);
    }

    /**
     * Constructeur avec conditions aux limites spécifiées
     */
    public Maillage(int N_total, ConditionsLimites conditions) {
        this(N_total); // Appelle le constructeur principal
        this.conditionsLimites = conditions;

        // Application immédiate des conditions aux limites
        appliquerConditionsLimites();
    }

    /**
     * Initialise le masque des conditions aux limites
     * Les bords du domaine sont marqués comme conditions de Dirichlet
     */
    private void initializeBoundary() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                boundary[i][j] = (i == 0 || i == N_total - 1 || j == 0 || j == N_total - 1);
            }
        }
        // Application des conditions aux limites initiales
        appliquerConditionsLimites();
    }

    /**
     * Applique les conditions aux limites au maillage
     */
    public void appliquerConditionsLimites() {
        if (conditionsLimites != null) {
            // N_total est la taille totale du maillage
            conditionsLimites.appliquerConditions(U, N_total, h);

            // Vérification de la compatibilité
            if (!conditionsLimites.verifierCompatibilite()) {
                System.err.println("Attention: Conditions aux limites incompatibles aux coins!");
            }
        }
    }

    /**
     * Configure un cas de test spécifique avec ses conditions aux limites recommandées
     *
     * @param casTest Le cas de test à configurer
     */
    public void configurerCasTest(CasTest casTest) {
        configurerCasTest(casTest, null);
    }

    /**
     * Configure un cas de test avec conditions aux limites personnalisées
     *
     * @param casTest Le cas de test à configurer
     * @param conditionsPersonnalisees Conditions aux limites (null pour utiliser les recommandées)
     */
    public void configurerCasTest(CasTest casTest, ConditionsLimites conditionsPersonnalisees) {
        System.out.println("Configuration du " + casTest.name() + " : " + casTest.getDescription());

        // Configuration des conditions aux limites
        if (conditionsPersonnalisees != null) {
            this.conditionsLimites = conditionsPersonnalisees;
        } else {
            this.conditionsLimites = ConditionsLimites.creerConditionsTest(casTest.getConditionsRecommandees());
        }

        // Réinitialisation de la solution dans le domaine intérieur
        // Les points intérieurs vont de 1 à N_interieur (soit 1 à N_total-2)
        for (int i = 1; i <= N_interieur; i++) {
            for (int j = 1; j <= N_interieur; j++) {
                U[i][j] = 0.0;
            }
        }

        // Configuration du terme source et de la solution exacte
        // Les boucles parcourent TOUS les points du maillage (0 à N_total-1)
        switch (casTest) {
            case CAS1:
                configurerCas1();
                break;
            case CAS2:
                configurerCas2();
                break;
            case CAS3:
                configurerCas3();
                break;
            case CAS4:
                configurerCas4();
                break;
            case CAS5:
                configurerCas5();
                break;
            case CAS6:
                configurerCas6();
                break;
            case CAS7:
                configurerCas7();
                break;
            case CAS8:
                configurerCas8();
                break;
        }

        // Application des conditions aux limites
        appliquerConditionsLimites();

        System.out.println("Conditions aux limites: " + conditionsLimites.getDescription());
    }

    // Les méthodes configurerCasX parcourent tous les points du maillage de 0 à N_total-1
    private void configurerCas1() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                double x = j * h;
                double y = i * h;
                F[i][j] = -2 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.sin(Math.PI * y);
                exactSol[i][j] = Math.sin(Math.PI * x) * Math.sin(Math.PI * y);
            }
        }
    }

    private void configurerCas2() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                double x = j * h;
                double y = i * h;
                F[i][j] = -8 * Math.PI * Math.PI * Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
                exactSol[i][j] = Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
            }
        }
    }

    private void configurerCas3() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                F[i][j] = 1.0;
                exactSol[i][j] = 0.0; // Solution exacte dépend des CL
            }
        }
    }

    private void configurerCas4() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                double x = j * h;
                double y = i * h;
                F[i][j] = x * x + y * y;
                exactSol[i][j] = 0.0; // Solution exacte complexe
            }
        }
    }

    private void configurerCas5() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                double x = j * h;
                double y = i * h;
                F[i][j] = -2 * (x * x + y * y - x - y);
                exactSol[i][j] = x * (1 - x) * y * (1 - y);
            }
        }
    }

    private void configurerCas6() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                F[i][j] = 0.0;
                exactSol[i][j] = 0.0; // Solution dépend des CL
            }
        }
    }

    private void configurerCas7() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                double x = j * h;
                double y = i * h;
                F[i][j] = 4 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.cos(Math.PI * y);
                exactSol[i][j] = 0.0; // Solution dépend des CL
            }
        }
    }

    private void configurerCas8() {
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                F[i][j] = 2.0;
                exactSol[i][j] = 0.0; // Solution dépend des CL
            }
        }
    }

    /**
     * Calcule la solution exacte modifiée pour tenir compte des conditions aux limites
     * Pour les cas où on connaît la solution de l'équation homogène mais pas avec CL générales
     */
    public void calculerSolutionExacteAvecCL() {
        System.out.println("Calcul de la solution exacte avec conditions aux limites générales...");
    }

    /**
     * Convertit les indices 2D (i,j) des points INTÉRIEURS en indice linéaire k
     * i et j vont de 1 à N_interieur.
     *
     * @param i Indice ligne intérieur (1 ≤ i ≤ N_interieur)
     * @param j Indice colonne intérieur (1 ≤ j ≤ N_interieur)
     * @return Indice linéaire k = (i-1)*N_interieur + (j-1)
     */
    public int indicesToLinear(int i, int j) {
        if (i < 1 || i > N_interieur || j < 1 || j > N_interieur) {
            throw new IllegalArgumentException("Indices (" + i + "," + j + ") hors du domaine intérieur [" +
                "1.." + N_interieur + ", 1.." + N_interieur + "]");
        }
        return (i - 1) * N_interieur + (j - 1);
    }

    /**
     * Convertit un indice linéaire k en indices 2D (i,j) pour les points INTÉRIEURS
     *
     * @param k Indice linéaire (0 ≤ k < N_interieur²)
     * @return Tableau [i, j] avec i = k/N_interieur + 1, j = k%N_interieur + 1
     */
    public int[] linearToIndices(int k) {
        if (k < 0 || k >= N_interieur * N_interieur) {
            throw new IllegalArgumentException("Indice linéaire " + k + " hors domaine [0.." +
                (N_interieur * N_interieur - 1) + "]");
        }
        int i = k / N_interieur + 1;
        int j = k % N_interieur + 1;
        return new int[]{i, j};
    }

    /**
     * Calcule les coordonnées physiques (x,y) à partir des indices globaux (i,j)
     * i et j vont de 0 à N_total-1.
     *
     * @param i Indice ligne global
     * @param j Indice colonne global
     * @return Tableau [x, y] avec x = j*h, y = i*h
     */
    public double[] getCoordinates(int i, int j) {
        if (i < 0 || i >= N_total || j < 0 || j >= N_total) {
            throw new IllegalArgumentException("Indices (" + i + "," + j + ") hors du maillage total [0.." +
                (N_total - 1) + ", 0.." + (N_total - 1) + "]");
        }
        return new double[]{j * h, i * h};
    }

    /**
     * Vérifie si un point (indices globaux i,j) est sur le bord du domaine
     */
    public boolean estSurBord(int i, int j) {
        return (i == 0 || i == N_total - 1 || j == 0 || j == N_total - 1);
    }

    /**
     * Obtient la valeur de condition aux limites en un point du bord (indices globaux i,j)
     */
    public double getValeurConditionLimite(int i, int j) {
        if (!estSurBord(i, j)) {
            throw new IllegalArgumentException("Point (" + i + "," + j + ") n'est pas sur le bord");
        }
        // N_total est la taille totale du maillage
        return conditionsLimites.getValeurBord(i, j, N_total, h);
    }

    /**
     * Configure des conditions aux limites personnalisées
     */
    public void setConditionsLimites(ConditionsLimites nouvelles) {
        this.conditionsLimites = nouvelles;
        appliquerConditionsLimites();
        System.out.println("Nouvelles conditions aux limites appliquées: " + conditionsLimites.getDescription());
    }

    /**
     * Retourne une copie profonde de la solution actuelle
     * Utile pour sauvegarder l'état avant modifications
     *
     * @return Copie de la matrice U
     */
    public double[][] copierSolution() {
        double[][] copie = new double[N_total][N_total];
        for (int i = 0; i < N_total; i++) {
            System.arraycopy(U[i], 0, copie[i], 0, N_total);
        }
        return copie;
    }

    /**
     * Restaure une solution sauvegardée (en préservant les conditions aux limites)
     *
     * @param solution Solution à restaurer
     */
    public void restaurerSolution(double[][] solution) {
        // Restaurer les points intérieurs seulement (indices de 1 à N_interieur)
        for (int i = 1; i <= N_interieur; i++) {
            for (int j = 1; j <= N_interieur; j++) {
                U[i][j] = solution[i][j];
            }
        }
        // Réappliquer les conditions aux limites pour les bords
        appliquerConditionsLimites();
    }

    /**
     * Vérifie la cohérence du maillage
     */
    public boolean verifierCoherence() {
        boolean coherent = true;
        // Vérifier que les conditions aux limites sont appliquées
        for (int i = 0; i < N_total; i++) {
            for (int j = 0; j < N_total; j++) {
                if (boundary[i][j]) { // ou estSurBord(i,j)
                    double valeurAttendue = conditionsLimites.getValeurBord(i, j, N_total, h);
                    if (Math.abs(U[i][j] - valeurAttendue) > 1e-12) {
                        System.err.println("Incohérence CL au point (" + i + "," + j + "): " +
                            U[i][j] + " ≠ " + valeurAttendue);
                        coherent = false;
                    }
                }
            }
        }
        return coherent;
    }

    /**
     * Affiche des informations détaillées sur le maillage
     */
    public void afficherInfos() {
        System.out.println("=== Informations Maillage ===");
        System.out.println("Taille totale (N_total): " + N_total + "×" + N_total + " points");
        System.out.println("Points intérieurs (N_interieur): " + N_interieur + "×" + N_interieur);
        System.out.println("Pas h: " + h);
        System.out.println("Nombre total d'inconnues (points intérieurs): " + (N_interieur * N_interieur));
        System.out.println("Conditions aux limites: " + conditionsLimites.getDescription());
        System.out.println("Cohérence: " + (verifierCoherence() ? "OK" : "PROBLÈME"));
        System.out.println("============================");
    }

    /**
     * Affiche un aperçu de la solution actuelle
     */
    public void afficherSolution() {
        System.out.println("=== Aperçu de la solution ===");
        if (N_interieur == 0) {
            System.out.println("Pas de points intérieurs à afficher.");
        } else {
            // Afficher quelques points caractéristiques
            int milieu_interieur = N_interieur / 2 + 1; // Indice intérieur, convertir en global si besoin
            System.out.println("Valeurs caractéristiques:");
            System.out.println("  Centre approx. (" + milieu_interieur + "," + milieu_interieur + ") (indices intérieurs): " + String.format("%.6f", U[milieu_interieur][milieu_interieur]));
        }
        System.out.println("  Coins du domaine:");
        System.out.println("    (0,0): " + String.format("%.6f", U[0][0]));
        System.out.println("    (0," + (N_total - 1) + "): " + String.format("%.6f", U[0][N_total - 1])); // y=0, x=1 (max j)
        System.out.println("    (" + (N_total - 1) + ",0): " + String.format("%.6f", U[N_total - 1][0])); // y=1, x=0 (max i)
        System.out.println("    (" + (N_total - 1) + "," + (N_total - 1) + "): " + String.format("%.6f", U[N_total - 1][N_total - 1]));

        // Statistiques sur le domaine intérieur
        if (N_interieur > 0) {
            double min = Double.MAX_VALUE, max = Double.MIN_VALUE, somme = 0.0;
            for (int i = 1; i <= N_interieur; i++) {
                for (int j = 1; j <= N_interieur; j++) {
                    min = Math.min(min, U[i][j]);
                    max = Math.max(max, U[i][j]);
                    somme += U[i][j];
                }
            }
            double moyenne = somme / (N_interieur * N_interieur);

            System.out.println("  Domaine intérieur:");
            System.out.println("    Min: " + String.format("%.6f", min));
            System.out.println("    Max: " + String.format("%.6f", max));
            System.out.println("    Moyenne: " + String.format("%.6f", moyenne));
        } else {
            System.out.println("  Pas de domaine intérieur pour les statistiques.");
        }
        System.out.println("==============================");
    }

    // Getters
    public int getN_total() { return N_total; }
    public int getN_interieur() { return N_interieur; } // Utile pour les solveurs
    public double getH() { return h; }
    public double[][] getU() { return U; }
    public double[][] getF() { return F; }
    public double[][] getExactSol() { return exactSol; }
    public boolean[][] getBoundary() { return boundary; }
    public ConditionsLimites getConditionsLimites() { return conditionsLimites; }

    public void setU(int i, int j, double value) { // i, j sont des indices globaux
        if (i < 0 || i >= N_total || j < 0 || j >= N_total) {
            throw new IllegalArgumentException("Indices (" + i + "," + j + ") hors maillage.");
        }
        if (!boundary[i][j]) { // ou !estSurBord(i,j)
            U[i][j] = value;
        } else {
            // Pour les points du bord, vérifier la cohérence avec les CL
            double valeurCL = conditionsLimites.getValeurBord(i, j, N_total, h);
            if (Math.abs(value - valeurCL) > 1e-10) {
                System.err.println("Tentative de modification d'une CL au point (" + i + "," + j + ")" +
                    " Valeur proposée: " + value + ", Valeur CL: " + valeurCL);
                // On pourrait forcer la valeur CL ici, ou laisser comme avant.
                // Pour l'instant, on n'applique pas la modif si c'est un bord et différent de la CL.
            } else {
                U[i][j] = value; // Si c'est la bonne valeur CL, on l'applique.
            }
        }
    }

    public double getU(int i, int j) { return U[i][j]; } // i, j sont des indices globaux
    public double getF(int i, int j) { return F[i][j]; } // i, j sont des indices globaux
    public double getExactSol(int i, int j) { return exactSol[i][j]; } // i, j sont des indices globaux
    public boolean isBoundary(int i, int j) { return boundary[i][j]; } // i, j sont des indices globaux
}
