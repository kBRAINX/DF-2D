import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Classe implémentant les différentes variantes de la méthode de Gauss-Seidel
 * pour résoudre le système linéaire issu de la discrétisation par différences finies
 * avec conditions aux limites de Dirichlet générales
 *
 * L'équation -ΔU = f est discrétisée par le schéma 5 points :
 * (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] - 4*U[i,j])/h² = -f[i,j]
 *
 * Avec conditions aux limites générales, le terme source effectif devient :
 * f_eff[i,j] = f[i,j] + (CL_voisines)/h² pour les points près du bord
 */
public class SolveurGaussSeidel {

    // Paramètres de convergence
    private static final double TOLERANCE_DEFAULT = 1e-6;
    private static final int MAX_ITERATIONS_DEFAULT = 10000;

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
        public final String methodeUtilisee;

        public ResultatConvergence(int iterations, double erreurFinale, long tempsCalcul,
                                   boolean converge, double[] historiqueErreurs, String methode) {
            this.iterations = iterations;
            this.erreurFinale = erreurFinale;
            this.tempsCalcul = tempsCalcul;
            this.converge = converge;
            this.historiqueErreurs = historiqueErreurs;
            this.methodeUtilisee = methode;
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
        System.out.println("Conditions aux limites: " + maillage.getConditionsLimites().getDescription());
        System.out.println("Tolérance: " + tolerance + ", Max itérations: " + maxIterations);

        // Vérification de la cohérence du maillage
        if (!maillage.verifierCoherence()) {
            System.err.println("Attention: Maillage incohérent détecté!");
        }

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
     * Adaptée aux conditions aux limites générales
     *
     * Pour un point intérieur (i,j), la mise à jour devient :
     * U[i,j] = (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] + h²*f[i,j]) / 4
     *
     * Les termes U[voisin] incluent les valeurs des conditions aux limites
     * lorsque le voisin est sur le bord.
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

        System.out.println("Gauss-Seidel classique avec conditions aux limites générales");

        do {
            erreurMax = 0.0;

            // Balayage séquentiel de tous les points intérieurs
            for (int i = 1; i <= N; i++) {
                for (int j = 1; j <= N; j++) {
                    if (!maillage.isBoundary(i, j)) {
                        double ancienneValeur = U[i][j];

                        // Calcul du terme source effectif incluant les CL
                        double termeSource = h2 * F[i][j];

                        // Les valeurs voisines incluent automatiquement les CL
                        // car elles sont maintenues dans la matrice U
                        double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];

                        // Mise à jour selon le schéma 5 points
                        U[i][j] = (sommeVoisins + termeSource) / 4.0;

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
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1),
            "Gauss-Seidel Classique");
    }

    /**
     * Méthode de Gauss-Seidel avec relaxation (SOR)
     * Adaptée aux conditions aux limites générales
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

        System.out.println("Gauss-Seidel avec relaxation ω = " + omega);
        System.out.println("Conditions aux limites prises en compte automatiquement");

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
                        double termeSource = h2 * F[i][j];
                        double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];
                        double valeurGS = (sommeVoisins + termeSource) / 4.0;

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
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1),
            "Gauss-Seidel avec Relaxation (ω=" + omega + ")");
    }

    /**
     * Méthode de Gauss-Seidel parallélisée avec coloration rouge-noir
     * Adaptée aux conditions aux limites générales
     */
    private ResultatConvergence gaussSeidelParallele(Maillage maillage, double tolerance,
                                                     int maxIterations, Consumer<Integer> callbackProgres) {

        long debut = System.currentTimeMillis();
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        System.out.println("Gauss-Seidel parallélisé avec " + NB_THREADS + " threads");
        System.out.println("Coloration rouge-noir pour la parallélisation");

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
            java.util.Arrays.copyOf(historiqueErreurs, iteration + 1),
            "Gauss-Seidel Parallélisé (" + NB_THREADS + " threads)");
    }

    /**
     * Met à jour un ensemble de points (rouge ou noir) en parallèle
     * Prend en compte automatiquement les conditions aux limites
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
                        // Vérifier la couleur du point et qu'il n'est pas sur le bord
                        if ((i + j) % 2 == couleur && !maillage.isBoundary(i, j)) {
                            double ancienneValeur = U[i][j];

                            // Calcul du terme source effectif
                            double termeSource = h2 * F[i][j];

                            // Somme des voisins (inclut automatiquement les CL)
                            double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];

                            // Mise à jour selon le schéma 5 points
                            U[i][j] = (sommeVoisins + termeSource) / 4.0;

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
     * Calcule le résidu du système Ax = b
     * Utile pour vérifier la qualité de la solution
     */
    public double calculerResidu(Maillage maillage) {
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N = maillage.getN();
        double h = maillage.getH();
        double h2 = h * h;

        double residuMax = 0.0;
        double residuL2 = 0.0;
        int compteur = 0;

        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                if (!maillage.isBoundary(i, j)) {
                    // Calcul du résidu local: r = f - L(U)
                    // où L est l'opérateur de différences finies
                    double laplacienDiscret = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] - 4*U[i][j]) / h2;
                    double residu = Math.abs(F[i][j] - (-laplacienDiscret));

                    residuMax = Math.max(residuMax, residu);
                    residuL2 += residu * residu;
                    compteur++;
                }
            }
        }

        residuL2 = Math.sqrt(residuL2 / compteur);

        System.out.println("Résidu du système:");
        System.out.println("  Résidu max: " + String.format("%.2e", residuMax));
        System.out.println("  Résidu L2: " + String.format("%.2e", residuL2));

        return residuMax;
    }

    /**
     * Teste la convergence pour différentes valeurs de ω (SOR)
     */
    public void testerParametresRelaxation(Maillage maillage, double[] valeurs_omega) {
        System.out.println("=== Test des paramètres de relaxation ===");

        double[][] solutionInitiale = maillage.copierSolution();

        for (double omega : valeurs_omega) {
            System.out.println("\nTest avec ω = " + omega);

            // Restaurer l'état initial
            maillage.restaurerSolution(solutionInitiale);

            // Test rapide avec peu d'itérations
            ResultatConvergence resultat = resoudre(maillage, MethodeResolution.GAUSS_SEIDEL_RELAXATION,
                1e-4, 1000, omega, null);

            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));
            System.out.println("  Convergé: " + resultat.converge);

            if (resultat.converge) {
                double facteur = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
                System.out.println("  Facteur de convergence: " + String.format("%.4f", facteur));
            }
        }

        // Restaurer l'état initial
        maillage.restaurerSolution(solutionInitiale);
    }

    /**
     * Effectue une analyse de performance comparative
     */
    public void analyserPerformances(Maillage maillage) {
        System.out.println("=== Analyse de performances ===");

        double[][] solutionInitiale = maillage.copierSolution();
        double tolerance = 1e-6;
        int maxIter = 5000;

        MethodeResolution[] methodes = {
            MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
            MethodeResolution.GAUSS_SEIDEL_RELAXATION,
            MethodeResolution.GAUSS_SEIDEL_PARALLELE
        };

        for (MethodeResolution methode : methodes) {
            System.out.println("\n--- " + methode.getNom() + " ---");

            // Restaurer l'état initial
            maillage.restaurerSolution(solutionInitiale);

            double omega = (methode == MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                calculerOmegaOptimal(maillage.getN()) : 1.0;

            long debut = System.currentTimeMillis();
            ResultatConvergence resultat = resoudre(maillage, methode, tolerance, maxIter, omega, null);
            long fin = System.currentTimeMillis();

            System.out.println("Temps total: " + (fin - debut) + " ms");
            System.out.println("Itérations: " + resultat.iterations);
            System.out.println("Convergé: " + resultat.converge);
            System.out.println("Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            // Calcul du résidu
            double residu = calculerResidu(maillage);

            // Efficacité (itérations par seconde)
            double efficacite = resultat.iterations * 1000.0 / (fin - debut);
            System.out.println("Efficacité: " + String.format("%.1f", efficacite) + " iter/s");
        }

        maillage.restaurerSolution(solutionInitiale);
    }

    /**
     * Ferme le pool de threads
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
     * Méthodes utilitaires pour résoudre avec paramètres par défaut
     */
    public ResultatConvergence resoudre(Maillage maillage, MethodeResolution methode) {
        return resoudre(maillage, methode, TOLERANCE_DEFAULT, MAX_ITERATIONS_DEFAULT, 1.0, null);
    }

    public ResultatConvergence resoudreSOR(Maillage maillage, double omega) {
        return resoudre(maillage, MethodeResolution.GAUSS_SEIDEL_RELAXATION,
            TOLERANCE_DEFAULT, MAX_ITERATIONS_DEFAULT, omega, null);
    }

    /**
     * Calcule le paramètre optimal de relaxation pour SOR
     * Formule adaptée aux conditions aux limites générales
     */
    public static double calculerOmegaOptimal(int N) {
        // Pour le problème de Poisson 2D avec différences finies:
        // ω_optimal ≈ 2 / (1 + sin(π/(N+1)))
        // Cette formule reste valide pour les conditions de Dirichlet générales
        double omega = 2.0 / (1.0 + Math.sin(Math.PI / (N + 1)));
        System.out.println("Paramètre ω optimal calculé: " + String.format("%.4f", omega) + " pour N=" + N);
        return omega;
    }

    /**
     * Estime le nombre de conditionnement de la matrice
     * (approximation basée sur les valeurs propres théoriques)
     */
    public static double NombreConditionnement(int N) {
        // Pour la matrice de différences finies 2D:
        // cond(A) ≈ (2/h²) * (1/sin²(π*h/2)) ≈ 8/π² * (N+1)²
        double h = 1.0 / (N + 1);
        double cond = 8.0 / (Math.PI * Math.PI) * (N + 1) * (N + 1);

        System.out.println("Nombre de conditionnement estimé: " + String.format("%.2e", cond));
        return cond;
    }
}
