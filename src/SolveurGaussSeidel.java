import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Classe implémentant les différentes variantes de la méthode de Gauss-Seidel
 * pour résoudre le système linéaire issu de la discrétisation par différences finies
 *
 * L'équation -ΔU = f est discrétisée par le schéma 5 points :
 * (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] - 4*U[i,j])/h² = -f[i,j]
 *
 * Réarrangé en forme itérative :
 * U[i,j] = (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] + h²*f[i,j]) / 4
 */
public class SolveurGaussSeidel {

    // Paramètres de convergence
    private static final double TOLERANCE_DEFAULT = 1e-6;
    private static final int MAX_ITERATIONS_DEFAULT = 1000;

    // Pool de threads pour la parallélisation
    private static final int NB_THREADS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService;

    /**
     * Énumération des méthodes de résolution disponibles
     */
    public enum MethodeResolution {
        GAUSS_SEIDEL_CLASSIQUE("Gauss-Seidel Classique"),
        GAUSS_SEIDEL_RELAXATION("Gauss-Seidel avec Relaxation (SOR)"),
        GAUSS_SEIDEL_PARALLELE("Gauss-Seidel Parallélisé");

        private final String nom;

        MethodeResolution(String nom) {
            this.nom = nom;
        }

        public String getNom() { return nom; }
    }

    /**
     * Classe pour stocker les résultats de convergence
     */
    public static class ResultatConvergence {
        public final int iterations;
        public final double erreurFinale;
        public final long tempsCalcul;
        public final boolean converge;
        public final double[] historiqueErreurs;

        public ResultatConvergence(int iterations, double erreurFinale, long tempsCalcul,
                                   boolean converge, double[] historiqueErreurs) {
            this.iterations = iterations;
            this.erreurFinale = erreurFinale;
            this.tempsCalcul = tempsCalcul;
            this.converge = converge;
            this.historiqueErreurs = historiqueErreurs;
        }
    }

    /**
     * Constructeur
     */
    public SolveurGaussSeidel() {
        this.executorService = Executors.newFixedThreadPool(NB_THREADS);
        System.out.println("Solveur Gauss-Seidel initialisé avec " + NB_THREADS + " threads");
    }

    /**
     * Résout le système avec la méthode spécifiée
     *
     * @param maillage Le maillage contenant le problème
     * @param methode La méthode de résolution à utiliser
     * @param tolerance Tolérance pour la convergence
     * @param maxIterations Nombre maximum d'itérations
     * @param omega Paramètre de relaxation (pour SOR, ignoré sinon)
     * @param callbackProgres Callback pour signaler le progrès (peut être null)
     * @return Résultat de la convergence
     */
    public ResultatConvergence resoudre(Maillage maillage, MethodeResolution methode,
                                        double tolerance, int maxIterations, double omega,
                                        Consumer<Integer> callbackProgres) {

        System.out.println("Début résolution avec " + methode.getNom());
        System.out.println("Tolérance: " + tolerance + ", Max itérations: " + maxIterations);

        switch (methode) {
            case GAUSS_SEIDEL_CLASSIQUE:
                return gaussSeidelClassique(maillage, tolerance, maxIterations, callbackProgres);

            case GAUSS_SEIDEL_RELAXATION:
                return gaussSeidelAvecRelaxation(maillage, tolerance, maxIterations, omega, callbackProgres);

            case GAUSS_SEIDEL_PARALLELE:
                return gaussSeidelParallele(maillage, tolerance, maxIterations, callbackProgres);

            default:
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
        }
    }

    /**
     * Méthode de Gauss-Seidel classique (séquentielle)
     *
     * Principe: Pour chaque point (i,j), on met à jour U[i,j] en utilisant
     * les valeurs les plus récentes des points voisins.
     *
     * Formule: U[i,j] = (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] + h²*f[i,j]) / 4
     *
     * @param maillage Le maillage à résoudre
     * @param tolerance Tolérance pour l'arrêt
     * @param maxIterations Nombre maximum d'itérations
     * @param callbackProgres Callback de progression
     * @return Résultat de la convergence
     */
    private ResultatConvergence gaussSeidelClassique(Maillage maillage, double tolerance,
                                                     int maxIterations, Consumer<Integer> callbackProgres) {

        long debut = System.currentTimeMillis();
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        double[] historiqueErreurs = new double[maxIterations + 1];
        int iteration = 0;
        double erreurMax;

        do {
            erreurMax = 0.0;

            // Balayage séquentiel de tous les points intérieurs
            for (int i = 1; i <= N; i++) {
                for (int j = 1; j <= N; j++) {
                    if (!maillage.isBoundary(i, j)) {
                        double ancienneValeur = U[i][j];

                        // Mise à jour selon le schéma 5 points
                        U[i][j] = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] + h2 * F[i][j]) / 4.0;

                        // Calcul de l'erreur locale
                        double erreur = Math.abs(U[i][j] - ancienneValeur);
                        erreurMax = Math.max(erreurMax, erreur);
                    }
                }
            }

            iteration++;
            historiqueErreurs[iteration] = erreurMax;

            // Callback de progression
            if (callbackProgres != null && iteration % 100 == 0) {
                callbackProgres.accept(iteration);
            }

        } while (erreurMax > tolerance && iteration < maxIterations);

        long fin = System.currentTimeMillis();
        boolean converge = erreurMax <= tolerance;

        System.out.println("Gauss-Seidel classique terminé:");
        System.out.println("  Itérations: " + iteration);
        System.out.println("  Erreur finale: " + erreurMax);
        System.out.println("  Convergé: " + converge);
        System.out.println("  Temps: " + (fin - debut) + " ms");

        return new ResultatConvergence(iteration, erreurMax, fin - debut, converge,
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1));
    }

    /**
     * Méthode de Gauss-Seidel avec relaxation (SOR - Successive Over-Relaxation)
     *
     * Principe: On applique un facteur de relaxation ω pour accélérer ou stabiliser
     * la convergence.
     *
     * Formule: U_new[i,j] = (1-ω)*U_old[i,j] + ω*U_GS[i,j]
     * où U_GS[i,j] est la valeur calculée par Gauss-Seidel standard
     *
     * - ω = 1: Gauss-Seidel classique
     * - 1 < ω < 2: Sur-relaxation (accélération)
     * - 0 < ω < 1: Sous-relaxation (stabilisation)
     *
     * @param maillage Le maillage à résoudre
     * @param tolerance Tolérance pour l'arrêt
     * @param maxIterations Nombre maximum d'itérations
     * @param omega Paramètre de relaxation
     * @param callbackProgres Callback de progression
     * @return Résultat de la convergence
     */
    private ResultatConvergence gaussSeidelAvecRelaxation(Maillage maillage, double tolerance,
                                                          int maxIterations, double omega,
                                                          Consumer<Integer> callbackProgres) {

        long debut = System.currentTimeMillis();
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        System.out.println("Paramètre de relaxation ω = " + omega);

        double[] historiqueErreurs = new double[maxIterations + 1];
        int iteration = 0;
        double erreurMax;

        do {
            erreurMax = 0.0;

            for (int i = 1; i <= N; i++) {
                for (int j = 1; j <= N; j++) {
                    if (!maillage.isBoundary(i, j)) {
                        double ancienneValeur = U[i][j];

                        // Calcul de la valeur Gauss-Seidel standard
                        double valeurGS = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] + h2 * F[i][j]) / 4.0;

                        // Application de la relaxation
                        U[i][j] = (1.0 - omega) * ancienneValeur + omega * valeurGS;

                        double erreur = Math.abs(U[i][j] - ancienneValeur);
                        erreurMax = Math.max(erreurMax, erreur);
                    }
                }
            }

            iteration++;
            historiqueErreurs[iteration] = erreurMax;

            if (callbackProgres != null && iteration % 100 == 0) {
                callbackProgres.accept(iteration);
            }

        } while (erreurMax > tolerance && iteration < maxIterations);

        long fin = System.currentTimeMillis();
        boolean converge = erreurMax <= tolerance;

        System.out.println("Gauss-Seidel avec relaxation terminé:");
        System.out.println("  ω = " + omega);
        System.out.println("  Itérations: " + iteration);
        System.out.println("  Erreur finale: " + erreurMax);
        System.out.println("  Convergé: " + converge);
        System.out.println("  Temps: " + (fin - debut) + " ms");

        return new ResultatConvergence(iteration, erreurMax, fin - debut, converge,
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1));
    }

    /**
     * Méthode de Gauss-Seidel parallélisée
     *
     * Principe: On utilise un schéma de coloration rouge-noir (damier) pour permettre
     * la parallélisation. Les points "rouges" et "noirs" peuvent être mis à jour
     * en parallèle car ils ne dépendent pas les uns des autres dans le schéma 5 points.
     *
     * Coloration: (i+j) % 2 == 0 → Rouge, (i+j) % 2 == 1 → Noir
     *
     * Algorithme:
     * 1. Mettre à jour tous les points rouges en parallèle
     * 2. Synchronisation
     * 3. Mettre à jour tous les points noirs en parallèle
     * 4. Répéter jusqu'à convergence
     *
     * @param maillage Le maillage à résoudre
     * @param tolerance Tolérance pour l'arrêt
     * @param maxIterations Nombre maximum d'itérations
     * @param callbackProgres Callback de progression
     * @return Résultat de la convergence
     */
    private ResultatConvergence gaussSeidelParallele(Maillage maillage, double tolerance,
                                                     int maxIterations, Consumer<Integer> callbackProgres) {

        long debut = System.currentTimeMillis();
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        System.out.println("Utilisation de " + NB_THREADS + " threads pour la parallélisation");

        double[] historiqueErreurs = new double[maxIterations + 1];
        int iteration = 0;
        double erreurMax;

        do {
            // Phase 1: Mise à jour des points rouges en parallèle
            double erreurRouge = mettreAJourPointsParallele(maillage, 0, true); // Rouge: (i+j) % 2 == 0

            // Phase 2: Mise à jour des points noirs en parallèle
            double erreurNoir = mettreAJourPointsParallele(maillage, 1, true); // Noir: (i+j) % 2 == 1

            erreurMax = Math.max(erreurRouge, erreurNoir);
            iteration++;
            historiqueErreurs[iteration] = erreurMax;

            if (callbackProgres != null && iteration % 100 == 0) {
                callbackProgres.accept(iteration);
            }

        } while (erreurMax > tolerance && iteration < maxIterations);

        long fin = System.currentTimeMillis();
        boolean converge = erreurMax <= tolerance;

        System.out.println("Gauss-Seidel parallélisé terminé:");
        System.out.println("  Threads utilisés: " + NB_THREADS);
        System.out.println("  Itérations: " + iteration);
        System.out.println("  Erreur finale: " + erreurMax);
        System.out.println("  Convergé: " + converge);
        System.out.println("  Temps: " + (fin - debut) + " ms");

        return new ResultatConvergence(iteration, erreurMax, fin - debut, converge,
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1));
    }

    /**
     * Met à jour un ensemble de points (rouge ou noir) en parallèle
     *
     * @param maillage Le maillage
     * @param couleur 0 pour rouge (i+j pair), 1 pour noir (i+j impair)
     * @param calculerErreur Si true, calcule l'erreur maximale
     * @return L'erreur maximale observée
     */
    private double mettreAJourPointsParallele(Maillage maillage, int couleur, boolean calculerErreur) {
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        // Division du travail entre threads
        Future<Double>[] futures = new Future[NB_THREADS];

        for (int t = 0; t < NB_THREADS; t++) {
            final int threadId = t;

            futures[t] = executorService.submit(() -> {
                double erreurLocale = 0.0;

                // Chaque thread traite une partie des lignes
                int debutLigne = 1 + threadId * N / NB_THREADS;
                int finLigne = 1 + Math.min((threadId + 1) * N / NB_THREADS, N);

                for (int i = debutLigne; i <= finLigne; i++) {
                    for (int j = 1; j <= N; j++) {
                        // Vérifier la couleur du point
                        if ((i + j) % 2 == couleur && !maillage.isBoundary(i, j)) {
                            double ancienneValeur = U[i][j];

                            // Mise à jour selon le schéma 5 points
                            U[i][j] = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] + h2 * F[i][j]) / 4.0;

                            if (calculerErreur) {
                                double erreur = Math.abs(U[i][j] - ancienneValeur);
                                erreurLocale = Math.max(erreurLocale, erreur);
                            }
                        }
                    }
                }

                return erreurLocale;
            });
        }

        // Attendre tous les threads et collecter l'erreur maximale
        double erreurMax = 0.0;
        try {
            for (Future<Double> future : futures) {
                erreurMax = Math.max(erreurMax, future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Erreur dans la parallélisation: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return erreurMax;
    }

    /**
     * Ferme le pool de threads
     * À appeler avant la fermeture de l'application
     */
    public void fermer() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Méthode utilitaire pour résoudre avec paramètres par défaut
     */
    public ResultatConvergence resoudre(Maillage maillage, MethodeResolution methode) {
        return resoudre(maillage, methode, TOLERANCE_DEFAULT, MAX_ITERATIONS_DEFAULT, 1.0, null);
    }

    /**
     * Méthode utilitaire pour résoudre avec SOR et paramètre omega personnalisé
     */
    public ResultatConvergence resoudreSOR(Maillage maillage, double omega) {
        return resoudre(maillage, MethodeResolution.GAUSS_SEIDEL_RELAXATION,
            TOLERANCE_DEFAULT, MAX_ITERATIONS_DEFAULT, omega, null);
    }

    /**
     * Calcule le paramètre optimal de relaxation pour SOR (approximation)
     * Basé sur la théorie pour les matrices de différences finies 2D
     *
     * @param N Taille du maillage
     * @return Paramètre omega optimal approximatif
     */
    public static double calculerOmegaOptimal(int N) {
        // Pour le problème de Poisson 2D avec conditions de Dirichlet sur les bords, ω_optimal ≈ 2 / (1 + sin(π/N))
        double omega = 2.0 / (1.0 + Math.sin(Math.PI / N));
        System.out.println("Paramètre ω optimal calculé: " + omega + " pour N=" + N);
        return omega;
    }
}
