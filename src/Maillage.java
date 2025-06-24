/**
 * Classe représentant le maillage 2D pour la résolution par différences finies
 * avec conditions aux limites de Dirichlet générales
 *
 * Cette classe gère :
 * - La discrétisation du domaine [0,1]×[0,1]
 * - Les conditions aux limites de Dirichlet générales U(bord) = valeurs spécifiées
 * - Les fonctions de test et solutions exactes
 * - La conversion entre indices 2D et indices linéaires
 *
 * Le maillage utilise un schéma de différences finies à 5 points sur une grille uniforme.
 */
public class Maillage {

    // Paramètres du maillage
    private int N;                    // Nombre de points intérieurs dans chaque direction
    private double h;                 // Pas du maillage h = 1/(N+1)
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
     * @param N Nombre de points intérieurs dans chaque direction
     */
    public Maillage(int N) {
        this.N = N;
        this.h = 1.0 / (N + 1);

        // Initialisation des tableaux (N+2)×(N+2) pour inclure les bords
        this.U = new double[N+2][N+2];
        this.F = new double[N+2][N+2];
        this.exactSol = new double[N+2][N+2];
        this.boundary = new boolean[N+2][N+2];

        // Conditions aux limites par défaut (homogènes)
        this.conditionsLimites = new ConditionsLimites();

        // Marquage des points frontière
        initializeBoundary();

        System.out.println("Maillage initialisé : " + (N+2) + "×" + (N+2) + " points, h = " + h);
    }

    /**
     * Constructeur avec conditions aux limites spécifiées
     */
    public Maillage(int N, ConditionsLimites conditions) {
        this(N);
        this.conditionsLimites = conditions;

        // Application immédiate des conditions aux limites
        appliquerConditionsLimites();
    }

    /**
     * Initialise le masque des conditions aux limites
     * Les bords du domaine sont marqués comme conditions de Dirichlet
     */
    private void initializeBoundary() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                boundary[i][j] = (i == 0 || i == N+1 || j == 0 || j == N+1);
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
            conditionsLimites.appliquerConditions(U, N, h);

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
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                U[i][j] = 0.0;
            }
        }

        // Configuration du terme source et de la solution exacte
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

    /**
     * Cas 1: f = -2π²sin(πx)sin(πy), solution exacte U = sin(πx)sin(πy)
     * Ce cas nécessite des conditions homogènes pour que la solution exacte soit valide
     */
    private void configurerCas1() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = j * h;
                double y = i * h;

                // Terme source f(x,y)
                F[i][j] = -2 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.sin(Math.PI * y);

                // Solution exacte (valide seulement avec conditions homogènes)
                exactSol[i][j] = Math.sin(Math.PI * x) * Math.sin(Math.PI * y);
            }
        }
    }

    /**
     * Cas 2: f = -8π²sin(2πx)sin(2πy), solution exacte U = sin(2πx)sin(2πy)
     */
    private void configurerCas2() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = j * h;
                double y = i * h;

                F[i][j] = -8 * Math.PI * Math.PI * Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
                exactSol[i][j] = Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
            }
        }
    }

    /**
     * Cas 3: f = 1 (constante)
     * Solution dépend des conditions aux limites
     */
    private void configurerCas3() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                F[i][j] = 1.0;
                exactSol[i][j] = 0.0; // Solution exacte dépend des CL
            }
        }
    }

    /**
     * Cas 4: f = x² + y²
     * Terme source polynomial
     */
    private void configurerCas4() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = j * h;
                double y = i * h;

                F[i][j] = x * x + y * y;
                exactSol[i][j] = 0.0; // Solution exacte complexe
            }
        }
    }

    /**
     * Cas 5: f = -2(x²+y²-x-y), solution exacte U = x(1-x)y(1-y)
     * Ce cas a une solution exacte qui s'annule naturellement sur le bord
     */
    private void configurerCas5() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = j * h;
                double y = i * h;

                F[i][j] = -2 * (x * x + y * y - x - y);
                exactSol[i][j] = x * (1 - x) * y * (1 - y);
            }
        }
    }

    /**
     * Cas 6: f = 0 (équation de Laplace)
     * Solution dépend entièrement des conditions aux limites
     */
    private void configurerCas6() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                F[i][j] = 0.0;
                exactSol[i][j] = 0.0; // Solution dépend des CL
            }
        }
    }

    /**
     * Cas 7: f = 4π²sin(πx)cos(πy)
     * Terme source mixte sinus/cosinus
     */
    private void configurerCas7() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = j * h;
                double y = i * h;

                F[i][j] = 4 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.cos(Math.PI * y);
                exactSol[i][j] = 0.0; // Solution dépend des CL
            }
        }
    }

    /**
     * Cas 8: f = 2 (constante)
     * Cas avec solution analytique simple pour certaines CL
     */
    private void configurerCas8() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
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
        // Cette méthode peut être étendue pour calculer des solutions exactes
        // en tenant compte des conditions aux limites spécifiques

        System.out.println("Calcul de la solution exacte avec conditions aux limites générales...");

        // Pour l'instant, on garde la solution exacte du cas homogène
        // Dans une version avancée, on pourrait résoudre analytiquement
        // ou utiliser des méthodes de superposition
    }

    /**
     * Convertit les indices 2D (i,j) en indice linéaire k
     * Utilisé pour la représentation matricielle du système linéaire
     *
     * @param i Indice ligne (1 ≤ i ≤ N)
     * @param j Indice colonne (1 ≤ j ≤ N)
     * @return Indice linéaire k = (i-1)*N + (j-1)
     */
    public int indicesToLinear(int i, int j) {
        if (i < 1 || i > N || j < 1 || j > N) {
            throw new IllegalArgumentException("Indices hors domaine intérieur: (" + i + "," + j + ")");
        }
        return (i - 1) * N + (j - 1);
    }

    /**
     * Convertit un indice linéaire k en indices 2D (i,j)
     *
     * @param k Indice linéaire (0 ≤ k < N²)
     * @return Tableau [i, j] avec i = k/N + 1, j = k%N + 1
     */
    public int[] linearToIndices(int k) {
        if (k < 0 || k >= N * N) {
            throw new IllegalArgumentException("Indice linéaire hors domaine: " + k);
        }
        int i = k / N + 1;
        int j = k % N + 1;
        return new int[]{i, j};
    }

    /**
     * Calcule les coordonnées physiques (x,y) à partir des indices (i,j)
     *
     * @param i Indice ligne
     * @param j Indice colonne
     * @return Tableau [x, y] avec x = j*h, y = i*h
     */
    public double[] getCoordinates(int i, int j) {
        return new double[]{j * h, i * h};
    }

    /**
     * Vérifie si un point est sur le bord du domaine
     */
    public boolean estSurBord(int i, int j) {
        return (i == 0 || i == N+1 || j == 0 || j == N+1);
    }

    /**
     * Obtient la valeur de condition aux limites en un point du bord
     */
    public double getValeurConditionLimite(int i, int j) {
        if (!estSurBord(i, j)) {
            throw new IllegalArgumentException("Point (" + i + "," + j + ") n'est pas sur le bord");
        }
        return conditionsLimites.getValeurBord(i, j, N, h);
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
        double[][] copie = new double[N+2][N+2];
        for (int i = 0; i <= N+1; i++) {
            System.arraycopy(U[i], 0, copie[i], 0, N+2);
        }
        return copie;
    }

    /**
     * Restaure une solution sauvegardée (en préservant les conditions aux limites)
     *
     * @param solution Solution à restaurer
     */
    public void restaurerSolution(double[][] solution) {
        // Restaurer les points intérieurs seulement
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                U[i][j] = solution[i][j];
            }
        }
        // Réappliquer les conditions aux limites
        appliquerConditionsLimites();
    }

    /**
     * Vérifie la cohérence du maillage
     */
    public boolean verifierCoherence() {
        boolean coherent = true;

        // Vérifier que les conditions aux limites sont appliquées
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                if (boundary[i][j]) {
                    double valeurAttendue = conditionsLimites.getValeurBord(i, j, N, h);
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
        System.out.println("Taille: " + (N+2) + "×" + (N+2) + " points");
        System.out.println("Points intérieurs: " + N + "×" + N);
        System.out.println("Pas h: " + h);
        System.out.println("Nombre total d'inconnues: " + (N*N));
        System.out.println("Conditions aux limites: " + conditionsLimites.getDescription());
        System.out.println("Cohérence: " + (verifierCoherence() ? "OK" : "PROBLÈME"));
        System.out.println("============================");
    }

    /**
     * Affiche un aperçu de la solution actuelle
     */
    public void afficherSolution() {
        System.out.println("=== Aperçu de la solution ===");

        // Afficher quelques points caractéristiques
        int milieu = N / 2 + 1;
        System.out.println("Valeurs caractéristiques:");
        System.out.println("  Centre (" + milieu + "," + milieu + "): " + String.format("%.6f", U[milieu][milieu]));
        System.out.println("  Coins du domaine:");
        System.out.println("    (0,0): " + String.format("%.6f", U[0][0]));
        System.out.println("    (0," + (N+1) + "): " + String.format("%.6f", U[0][N+1]));
        System.out.println("    (" + (N+1) + ",0): " + String.format("%.6f", U[N+1][0]));
        System.out.println("    (" + (N+1) + "," + (N+1) + "): " + String.format("%.6f", U[N+1][N+1]));

        // Statistiques sur le domaine intérieur
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, somme = 0.0;
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                min = Math.min(min, U[i][j]);
                max = Math.max(max, U[i][j]);
                somme += U[i][j];
            }
        }
        double moyenne = somme / (N * N);

        System.out.println("  Domaine intérieur:");
        System.out.println("    Min: " + String.format("%.6f", min));
        System.out.println("    Max: " + String.format("%.6f", max));
        System.out.println("    Moyenne: " + String.format("%.6f", moyenne));
        System.out.println("==============================");
    }

    // Getters et setters
    public int getN() { return N; }
    public double getH() { return h; }
    public double[][] getU() { return U; }
    public double[][] getF() { return F; }
    public double[][] getExactSol() { return exactSol; }
    public boolean[][] getBoundary() { return boundary; }
    public ConditionsLimites getConditionsLimites() { return conditionsLimites; }

    public void setU(int i, int j, double value) {
        if (!boundary[i][j]) {
            U[i][j] = value;
        } else {
            // Pour les points du bord, vérifier la cohérence avec les CL
            double valeurCL = conditionsLimites.getValeurBord(i, j, N, h);
            if (Math.abs(value - valeurCL) > 1e-10) {
                System.err.println("Tentative de modification d'une CL au point (" + i + "," + j + ")");
            }
        }
    }

    public double getU(int i, int j) { return U[i][j]; }
    public double getF(int i, int j) { return F[i][j]; }
    public double getExactSol(int i, int j) { return exactSol[i][j]; }
    public boolean isBoundary(int i, int j) { return boundary[i][j]; }
}
