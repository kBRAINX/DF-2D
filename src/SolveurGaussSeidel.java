import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.Arrays; // Ajout pour Arrays.copyOf

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
 *
 * Les calculs se font sur les points intérieurs, dont les indices vont de 1 à N_interieur.
 * N_interieur = N_total - 2.
 */
public class SolveurGaussSeidel {

    // Paramètres de convergence
    private static final double TOLERANCE_DEFAULT = 1e-6;
    private static final int MAX_ITERATIONS_DEFAULT = 10000;

    // Pool de threads pour la parallélisation
    private static final int NB_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors()); // Assurer au moins 1 thread
    private final ExecutorService executorService;

    /**
     * Énumération des méthodes de résolution disponibles
     */
    public enum MethodeResolution {
        GAUSS_SEIDEL_CLASSIQUE("Gauss-Seidel Classique"),
        GAUSS_SEIDEL_RELAXATION("Gauss-Seidel (SOR)"),
        GAUSS_SEIDEL_PARALLELE("Gauss-Seidel Par");

        private final String nom;
        MethodeResolution(String nom) { this.nom = nom; }
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
        if (maillage.getN_interieur() == 0) {
            System.out.println("Aucun point intérieur à résoudre. Convergence immédiate.");
            return new ResultatConvergence(0, 0.0, 0, true, new double[1], methode.getNom());
        }


        // Vérification de la cohérence du maillage
        if (!maillage.verifierCoherence()) {
            System.err.println("Attention: Maillage incohérent détecté avant résolution!");
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
     * Pour un point intérieur (i,j) (indices globaux), la mise à jour devient :
     * U[i,j] = (U[i-1,j] + U[i+1,j] + U[i,j-1] + U[i,j+1] + h²*f[i,j]) / 4
     * Les indices i et j pour les points intérieurs vont de 1 à N_interieur (N_total-2)
     */
    private ResultatConvergence gaussSeidelClassique(Maillage maillage, double tolerance,
                                                     int maxIterations, Consumer<Integer> callbackProgres) {
        long debut = System.currentTimeMillis();
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N_interieur = maillage.getN_interieur(); // Nombre de points intérieurs
        double h = maillage.getH();
        double h2 = h * h;

        double[] historiqueErreurs = new double[maxIterations + 1];
        int iteration = 0;
        double erreurMax;

        System.out.println("Gauss-Seidel classique avec conditions aux limites générales. N_interieur=" + N_interieur);
        if (N_interieur == 0) { // Pas de points intérieurs
            return new ResultatConvergence(0, 0.0, System.currentTimeMillis() - debut, true, new double[1], "Gauss-Seidel Classique");
        }

        do {
            erreurMax = 0.0;
            // Balayage séquentiel des points intérieurs.
            // Les indices i, j ici sont les indices GLOBAUX du maillage.
            // Les points intérieurs ont des indices globaux de 1 à N_total-2 (soit N_interieur).
            for (int i = 1; i <= N_interieur; i++) { // Ligne globale
                for (int j = 1; j <= N_interieur; j++) { // Colonne globale
                    // On est sûr que (i,j) est un point intérieur, pas besoin de !maillage.isBoundary(i,j)
                    double ancienneValeur = U[i][j];
                    double termeSource = h2 * F[i][j];
                    double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];
                    U[i][j] = (sommeVoisins + termeSource) / 4.0;
                    double erreur = Math.abs(U[i][j] - ancienneValeur);
                    erreurMax = Math.max(erreurMax, erreur);
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

        System.out.println("Gauss-Seidel classique terminé:");
        System.out.println("  Itérations: " + iteration);
        System.out.println("  Erreur finale: " + erreurMax);
        System.out.println("  Convergé: " + converge);
        System.out.println("  Temps: " + (fin - debut) + " ms");

        return new ResultatConvergence(iteration, erreurMax, fin - debut, converge,
            Arrays.copyOf(historiqueErreurs, iteration + 1),
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
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();
        double h2 = h * h;

        System.out.println("Gauss-Seidel avec relaxation ω = " + omega + ". N_interieur=" + N_interieur);
        if (N_interieur == 0) {
            return new ResultatConvergence(0, 0.0, System.currentTimeMillis() - debut, true, new double[1], "Gauss-Seidel avec Relaxation (ω=" + omega + ")");
        }

        double[] historiqueErreurs = new double[maxIterations + 1];
        int iteration = 0;
        double erreurMax;

        do {
            erreurMax = 0.0;
            for (int i = 1; i <= N_interieur; i++) { // Ligne globale
                for (int j = 1; j <= N_interieur; j++) { // Colonne globale
                    double ancienneValeur = U[i][j];
                    double termeSource = h2 * F[i][j];
                    double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];
                    double valeurGS = (sommeVoisins + termeSource) / 4.0;
                    U[i][j] = (1.0 - omega) * ancienneValeur + omega * valeurGS;
                    double erreur = Math.abs(U[i][j] - ancienneValeur);
                    erreurMax = Math.max(erreurMax, erreur);
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
            Arrays.copyOf(historiqueErreurs, iteration + 1),
            "Gauss-Seidel avec Relaxation (ω=" + String.format("%.2f", omega) + ")");
    }

    /**
     * Méthode de Gauss-Seidel parallélisée avec coloration rouge-noir
     * Adaptée aux conditions aux limites générales
     */
    private ResultatConvergence gaussSeidelParallele(Maillage maillage, double tolerance,
                                                     int maxIterations, Consumer<Integer> callbackProgres) {
        long debut = System.currentTimeMillis();
        int N_interieur = maillage.getN_interieur();

        System.out.println("Gauss-Seidel parallélisé avec " + NB_THREADS + " threads. N_interieur=" + N_interieur);
        System.out.println("Coloration rouge-noir pour la parallélisation");
        if (N_interieur == 0) {
            return new ResultatConvergence(0, 0.0, System.currentTimeMillis() - debut, true, new double[1], "Gauss-Seidel Parallélisé (" + NB_THREADS + " threads)");
        }

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
            Arrays.copyOf(historiqueErreurs, iteration + 1),
            "Gauss-Seidel Parallélisé (" + NB_THREADS + " threads)");
    }

    /**
     * Met à jour un ensemble de points (rouge ou noir) en parallèle
     * Prend en compte automatiquement les conditions aux limites
     * Les indices i et j sont les indices GLOBAUX des points intérieurs.
     */
    private double mettreAJourPointsParallele(Maillage maillage, int couleur, boolean calculerErreur) {
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();
        double h2 = h * h;

        if (N_interieur == 0) return 0.0;

        Future<Double>[] futures = new Future[NB_THREADS];
        // Chaque thread traite un sous-ensemble des lignes intérieures (de 1 à N_interieur)
        int lignesParThread = (N_interieur + NB_THREADS - 1) / NB_THREADS;


        for (int t = 0; t < NB_THREADS; t++) {
            final int threadId = t;
            futures[t] = executorService.submit(() -> {
                double erreurLocaleMax = 0.0;
                // Déterminer les lignes globales à traiter par ce thread
                // Les lignes intérieures ont des indices globaux de 1 à N_interieur
                int debutLigneGlobale = 1 + threadId * lignesParThread;
                int finLigneGlobale = Math.min(N_interieur, debutLigneGlobale + lignesParThread - 1);

                for (int i = debutLigneGlobale; i <= finLigneGlobale; i++) { // i est un indice global
                    for (int j = 1; j <= N_interieur; j++) { // j est un indice global
                        // Vérifier la couleur du point (i,j)
                        // Note: les indices i,j ici sont les indices GLOBAUX du maillage.
                        // (i+j)%2 est basé sur les indices globaux.
                        if ((i + j) % 2 == couleur) {
                            // On est sûr que (i,j) est un point intérieur
                            double ancienneValeur = U[i][j];
                            double termeSource = h2 * F[i][j];
                            double sommeVoisins = U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1];
                            U[i][j] = (sommeVoisins + termeSource) / 4.0;

                            if (calculerErreur) {
                                double erreur = Math.abs(U[i][j] - ancienneValeur);
                                erreurLocaleMax = Math.max(erreurLocaleMax, erreur);
                            }
                        }
                    }
                }
                return erreurLocaleMax;
            });
        }

        double erreurMaxGlobale = 0.0;
        try {
            for (Future<Double> future : futures) {
                erreurMaxGlobale = Math.max(erreurMaxGlobale, future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Erreur dans la parallélisation: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        return erreurMaxGlobale;
    }


    /**
     * Calcule le résidu du système Ax = b pour les points intérieurs
     * Utile pour vérifier la qualité de la solution
     */
    public double calculerResidu(Maillage maillage) {
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();
        double h2 = h * h;

        if (N_interieur == 0) {
            System.out.println("Aucun point intérieur pour calculer le résidu.");
            return 0.0;
        }

        double residuMax = 0.0;
        double residuL2 = 0.0;
        int compteur = 0;

        // Boucles sur les indices GLOBAUX des points intérieurs
        for (int i = 1; i <= N_interieur; i++) {
            for (int j = 1; j <= N_interieur; j++) {
                // (i,j) est un point intérieur
                double laplacienDiscret = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] - 4*U[i][j]) / h2;
                double residu = Math.abs(F[i][j] - (-laplacienDiscret)); // -DeltaU = F  => F + DeltaU = 0

                residuMax = Math.max(residuMax, residu);
                residuL2 += residu * residu;
                compteur++;
            }
        }

        residuL2 = Math.sqrt(residuL2 / compteur); // RMS du résidu

        System.out.println("Résidu du système (points intérieurs):");
        System.out.println("  Résidu max: " + String.format("%.2e", residuMax));
        System.out.println("  Résidu L2 (RMS): " + String.format("%.2e", residuL2));
        return residuMax;
    }

    /**
     * Teste la convergence pour différentes valeurs de ω (SOR)
     */
    public void testerParametresRelaxation(Maillage maillage, double[] valeurs_omega) {
        System.out.println("=== Test des paramètres de relaxation ===");
        if (maillage.getN_interieur() == 0) {
            System.out.println("Aucun point intérieur, test annulé.");
            return;
        }

        double[][] solutionInitiale = maillage.copierSolution();

        for (double omega : valeurs_omega) {
            System.out.println("\nTest avec ω = " + omega);
            maillage.restaurerSolution(solutionInitiale); // Réinitialiser U pour chaque test

            ResultatConvergence resultat = resoudre(maillage, MethodeResolution.GAUSS_SEIDEL_RELAXATION,
                1e-4, 1000, omega, null);

            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));
            System.out.println("  Convergé: " + resultat.converge);

            if (resultat.converge && resultat.historiqueErreurs != null && resultat.historiqueErreurs.length > 2) {
                double facteur = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
                System.out.println("  Facteur de convergence: " + String.format("%.4f", facteur));
            }
        }
        maillage.restaurerSolution(solutionInitiale); // Restaurer après tous les tests
    }

    /**
     * Effectue une analyse de performance comparative
     */
    public void analyserPerformances(Maillage maillage) {
        System.out.println("=== Analyse de performances ===");
        if (maillage.getN_interieur() == 0) {
            System.out.println("Aucun point intérieur, analyse annulée.");
            return;
        }

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
            maillage.restaurerSolution(solutionInitiale);

            double omega = (methode == MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                calculerOmegaOptimal(maillage.getN_total()) : 1.0; // N_total pour omega

            long debut = System.currentTimeMillis();
            ResultatConvergence resultat = resoudre(maillage, methode, tolerance, maxIter, omega, null);
            long fin = System.currentTimeMillis();

            System.out.println("Temps total: " + (fin - debut) + " ms");
            System.out.println("Itérations: " + resultat.iterations);
            System.out.println("Convergé: " + resultat.converge);
            System.out.println("Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            calculerResidu(maillage); // Affiche ses propres infos

            if ((fin - debut) > 0) {
                double efficacite = resultat.iterations * 1000.0 / (fin - debut);
                System.out.println("Efficacité: " + String.format("%.1f", efficacite) + " iter/s");
            }
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
                Thread.currentThread().interrupt(); // Restore interrupted status
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
     * Formule adaptée aux conditions aux limites générales.
     * N_total est le nombre total de points (bords inclus).
     * La formule utilise le nombre d'intervalles, N_total - 1.
     */
    public static double calculerOmegaOptimal(int N_total) {
        if (N_total < 2) return 1.0; // Cas limite, devrait être N_total >= 3
        // La formule originale est souvent écrite avec N_interieur+1 ou N_pas_discretisation
        // Si N_total est le nb de points, alors il y a N_total-1 intervalles.
        // ω_optimal ≈ 2 / (1 + sin(π / (Nombre d'intervalles)))
        // Nombre d'intervalles = N_total - 1
        double nbIntervalles = N_total - 1;
        if (nbIntervalles <= 0) return 1.0; // Évite division par zéro
        double omega = 2.0 / (1.0 + Math.sin(Math.PI / nbIntervalles));
        System.out.println("Paramètre ω optimal calculé: " + String.format("%.4f", omega) + " pour N_total=" + N_total + " (nbIntervalles=" + nbIntervalles + ")");
        return omega;
    }

    /**
     * Estime le nombre de conditionnement de la matrice
     * (approximation basée sur les valeurs propres théoriques)
     * N_total est le nombre total de points.
     */
    public static double NombreConditionnement(int N_total) {
        if (N_total < 2) return 1.0;
        // cond(A) ≈ (2/h²) * (1/sin²(π*h/2)) ≈ 8/π² * (Nombre d'intervalles)²
        // Nombre d'intervalles = N_total - 1
        double nbIntervalles = N_total - 1;
        if (nbIntervalles <= 0) return 1.0;
        // double h = 1.0 / nbIntervalles; // Pas besoin de h explicitement
        double cond = 8.0 / (Math.PI * Math.PI) * nbIntervalles * nbIntervalles;

        System.out.println("Nombre de conditionnement estimé: " + String.format("%.2e", cond) + " pour N_total=" + N_total);
        return cond;
    }
}
