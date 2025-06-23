/**
 * Classe représentant le maillage 2D pour la résolution par différences finies
 *
 * Cette classe gère :
 * La discrétisation du domaine [0,1]×[0,1]
 * Les conditions aux limites
 * Les fonctions de test et solutions exactes
 * La conversion entre indices 2D et indices linéaires.
 *
 * Le maillage utilise un schéma de différences finies à 5 points sur une grille uniforme.
 */
public class Maillage {

    // Paramètres du maillage
    private int N;              // Nombre de points intérieurs dans chaque direction
    private double h;           // Pas du maillage h = 1/(N+1)
    private double[][] U;       // Solution U(x,y) aux points du maillage
    private double[][] F;       // Terme source f(x,y)
    private double[][] exactSol;
    private boolean[][] boundary; // Masque des conditions aux limites

    // Cas de test disponibles
    public enum CasTest {
        CAS1("f = -2π²sin(πx)sin(πy)", "U_exact = sin(πx)sin(πy)"),
        CAS2("f = -8π²sin(2πx)sin(2πy)", "U_exact = sin(2πx)sin(2πy)"),
        CAS3("f = 1 (constante)", "Solution inconnue"),
        CAS4("f = x² + y²", "Solution inconnue"),
        CAS5("f = -2(x²+y²-x-y)", "U_exact = x(1-x)y(1-y)");

        private final String description;
        private final String solutionExacte;

        CasTest(String desc, String sol) {
            this.description = desc;
            this.solutionExacte = sol;
        }

        public String getDescription() { return description; }
        public String getSolutionExacte() { return solutionExacte; }
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

        // Marquage des points frontière
        initializeBoundary();

        System.out.println("Maillage initialisé : " + (N+2) + "×" + (N+2) + " points, h = " + h);
    }

    /**
     * Initialise le masque des conditions aux limites
     * Les bords du domaine sont marqués comme conditions de Dirichlet (U = 0)
     */
    private void initializeBoundary() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                boundary[i][j] = (i == 0 || i == N+1 || j == 0 || j == N+1);
            }
        }

        // Application des conditions aux limites (U = 0 sur le bord)
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                if (boundary[i][j]) {
                    U[i][j] = 0.0;
                }
            }
        }
    }

    /**
     * Configure un cas de test spécifique
     *
     * @param casTest Le cas de test à configurer
     */
    public void configurerCasTest(CasTest casTest) {
        System.out.println("Configuration du " + casTest.name() + " : " + casTest.getDescription());

        // Réinitialisation de la solution dans le domaine intérieur
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                U[i][j] = 0.0;
            }
        }

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
        }
    }

    /**
     * Cas 1: f = -2π²sin(πx)sin(πy), solution exacte U = sin(πx)sin(πy)
     */
    private void configurerCas1() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = i * h;
                double y = j * h;

                // Terme source f(x,y)
                F[i][j] = -2 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.sin(Math.PI * y);

                // Solution exacte
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
                double x = i * h;
                double y = j * h;

                F[i][j] = -8 * Math.PI * Math.PI * Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
                exactSol[i][j] = Math.sin(2 * Math.PI * x) * Math.sin(2 * Math.PI * y);
            }
        }
    }

    /**
     * Cas 3: f = 1 (constante)
     */
    private void configurerCas3() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                F[i][j] = 1.0;
                exactSol[i][j] = 0.0; // Pas de solution exacte connue
            }
        }
    }

    /**
     * Cas 4: f = x² + y²
     */
    private void configurerCas4() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = i * h;
                double y = j * h;

                F[i][j] = x * x + y * y;
                exactSol[i][j] = 0.0; // Pas de solution exacte simple
            }
        }
    }

    /**
     * Cas 5: f = -2(x²+y²-x-y), solution exacte U = x(1-x)y(1-y)
     */
    private void configurerCas5() {
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                double x = i * h;
                double y = j * h;

                F[i][j] = -2 * (x * x + y * y - x - y);
                exactSol[i][j] = x * (1 - x) * y * (1 - y);
            }
        }
    }

    /**
     * Conversion des indices (i,j) en indice linéaire k
     *
     * @param i Indice ligne (1 ≤ i ≤ N)
     * @param j Indice colonne (1 ≤ j ≤ N)
     * @return Indice linéaire k = (i-1)*N + (j-1)
     */
    public int indicesToLinear(int i, int j) {
        return (i - 1) * N + (j - 1);
    }

    /**
     * Conversion d'un indice linéaire k en indices (i,j)
     *
     * @param k Indice linéaire (0 ≤ k < N²)
     * @return Tableau [i, j] avec i = k/N + 1, j = k%N + 1
     */
    public int[] linearToIndices(int k) {
        int i = k / N + 1;
        int j = k % N + 1;
        return new int[]{i, j};
    }

    /**
     * Calcule des coordonnées physiques (x,y) à partir des indices (i,j)
     *
     * @param i Indice ligne
     * @param j Indice colonne
     * @return Tableau [x, y] avec x = j*h, y = i*h
     */
    public double[] getCoordinates(int i, int j) {
        return new double[]{j * h, i * h};
    }

    // Getters et setters
    public int getN() { return N; }
    public double getH() { return h; }
    public double[][] getU() { return U; }
    public double[][] getF() { return F; }
    public double[][] getExactSol() { return exactSol; }
    public boolean[][] getBoundary() { return boundary; }

    public void setU(int i, int j, double value) {
        if (!boundary[i][j]) {
            U[i][j] = value;
        }
    }

    public double getU(int i, int j) { return U[i][j]; }
    public double getF(int i, int j) { return F[i][j]; }
    public double getExactSol(int i, int j) { return exactSol[i][j]; }
    public boolean isBoundary(int i, int j) { return boundary[i][j]; }

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
     * Restaure une solution sauvegardée
     *
     * @param solution Solution à restaurer
     */
    public void restaurerSolution(double[][] solution) {
        for (int i = 0; i <= N+1; i++) {
            System.arraycopy(solution[i], 0, U[i], 0, N+2);
        }
    }

    /**
     * Affiche des informations sur le maillage
     */
    public void afficherInfos() {
        System.out.println("=== Informations Maillage ===");
        System.out.println("Taille: " + (N+2) + "×" + (N+2) + " points");
        System.out.println("Points intérieurs: " + N + "×" + N);
        System.out.println("Pas h: " + h);
        System.out.println("Nombre total d'inconnues: " + (N*N));
        System.out.println("============================");
    }
}
