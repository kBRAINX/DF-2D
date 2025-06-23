/**
 * Classe pour l'analyse des erreurs et le calcul de l'ordre de convergence
 *
 * Cette classe implémente :
 * Calcul de l'erreur en norme f² par intégration numérique
 * Calcul de l'erreur en norme infinie (max)
 * Détermination de l'ordre numérique de convergence
 * Analyse de la convergence des méthodes itératives
 */
public class AnalyseurErreurs {

    /**
     * Classe pour stocker les résultats d'analyse d'erreur
     */
    public static class ResultatAnalyse {
        public final double erreurL2;
        public final double erreurMax;
        public final double erreurMoyenne;
        public final int pointsAnalyses;
        public final double[][] carteErreurs;

        public ResultatAnalyse(double erreurL2, double erreurMax, double erreurMoyenne,
                               int pointsAnalyses, double[][] carteErreurs) {
            this.erreurL2 = erreurL2;
            this.erreurMax = erreurMax;
            this.erreurMoyenne = erreurMoyenne;
            this.pointsAnalyses = pointsAnalyses;
            this.carteErreurs = carteErreurs;
        }
    }

    /**
     * Classe pour les résultats d'étude de convergence
     */
    public static class EtudeConvergence {
        public final double[] taillesMaillage;
        public final double[] erreursL2;
        public final double[] erreursMax;
        public final double ordreConvergenceL2;
        public final double ordreConvergenceMax;
        public final double[] ordresLocaux;

        public EtudeConvergence(double[] taillesMaillage, double[] erreursL2, double[] erreursMax,
                                double ordreConvergenceL2, double ordreConvergenceMax, double[] ordresLocaux) {
            this.taillesMaillage = taillesMaillage;
            this.erreursL2 = erreursL2;
            this.erreursMax = erreursMax;
            this.ordreConvergenceL2 = ordreConvergenceL2;
            this.ordreConvergenceMax = ordreConvergenceMax;
            this.ordresLocaux = ordresLocaux;
        }
    }

    /**
     * Calcule l'erreur entre la solution numérique et la solution exacte
     *
     * L'erreur f² est calculée par intégration numérique :
     * ||e||₂ = √(∫∫ |U_numerical - U_exact|² dx dy)
     *
     * Pour la discrétisation, on utilise la formule des trapèzes :
     * ||e||₂ ≈ √(h² * Σᵢⱼ |U[i,j] - U_exact[i,j]|²)
     *
     * @param maillage Le maillage contenant les solutions
     * @return Résultat de l'analyse d'erreur
     */
    public static ResultatAnalyse calculerErreurs(Maillage maillage) {
        double[][] U = maillage.getU();
        double[][] exactSol = maillage.getExactSol();
        int N = maillage.getN();
        double h = maillage.getH();

        // Carte des erreurs locales
        double[][] carteErreurs = new double[N+2][N+2];

        double sommeErreursCarrees = 0.0;
        double erreurMax = 0.0;
        double sommeErreurs = 0.0;
        int compteurPoints = 0;

        System.out.println("Calcul des erreurs par intégration numérique...");
        System.out.println("Pas du maillage h = " + h);

        // Parcours de tous les points intérieurs
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                if (!maillage.isBoundary(i, j)) {
                    // Erreur locale au point (i,j)
                    double erreurLocale = Math.abs(U[i][j] - exactSol[i][j]);
                    carteErreurs[i][j] = erreurLocale;

                    // Accumulation pour les différentes normes
                    sommeErreursCarrees += erreurLocale * erreurLocale;
                    erreurMax = Math.max(erreurMax, erreurLocale);
                    sommeErreurs += erreurLocale;
                    compteurPoints++;
                }
            }
        }

        // Calcul des normes
        // Norme f² avec intégration numérique (règle des trapèzes)
        double erreurL2 = Math.sqrt(h * h * sommeErreursCarrees);

        // Erreur moyenne
        double erreurMoyenne = sommeErreurs / compteurPoints;

        System.out.println("Erreurs calculées:");
        System.out.println("  Norme L²: " + String.format("%.6e", erreurL2));
        System.out.println("  Norme max: " + String.format("%.6e", erreurMax));
        System.out.println("  Erreur moyenne: " + String.format("%.6e", erreurMoyenne));
        System.out.println("  Points analysés: " + compteurPoints);

        return new ResultatAnalyse(erreurL2, erreurMax, erreurMoyenne, compteurPoints, carteErreurs);
    }

    /**
     * Effectue une étude de convergence en raffinant le maillage
     *
     * L'ordre de convergence p est estimé par :
     * Si ||e(h)|| ≈ C * h^p, alors p ≈ log(||e(h₁)||/||e(h₂)||) / log(h₁/h₂)
     *
     * @param casTest Le cas de test à analyser
     * @param taillesN Tableau des tailles de maillage à tester
     * @param methode Méthode de résolution à utiliser
     * @return Résultats de l'étude de convergence
     */
    public static EtudeConvergence etudierConvergence(Maillage.CasTest casTest, int[] taillesN,
                                                      SolveurGaussSeidel.MethodeResolution methode) {

        System.out.println("=== Étude de convergence ===");
        System.out.println("Cas de test: " + casTest.getDescription());
        System.out.println("Méthode: " + methode.getNom());
        System.out.println("Tailles de maillage: " + java.util.Arrays.toString(taillesN));

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();

        double[] taillesMaillage = new double[taillesN.length];
        double[] erreursL2 = new double[taillesN.length];
        double[] erreursMax = new double[taillesN.length];

        // Résolution pour chaque taille de maillage
        for (int k = 0; k < taillesN.length; k++) {
            int N = taillesN[k];
            System.out.println("\nRésolution pour N = " + N + " (maillage " + (N+2) + "×" + (N+2) + ")");

            // Création du maillage
            Maillage maillage = new Maillage(N);
            maillage.configurerCasTest(casTest);
            taillesMaillage[k] = maillage.getH();

            // Résolution
            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);

            if (!resultat.converge) {
                System.err.println("Attention: Non convergence pour N = " + N);
            }

            // Calcul des erreurs
            ResultatAnalyse analyse = calculerErreurs(maillage);
            erreursL2[k] = analyse.erreurL2;
            erreursMax[k] = analyse.erreurMax;

            System.out.println("  h = " + String.format("%.6f", taillesMaillage[k]));
            System.out.println("  Erreur L² = " + String.format("%.6e", erreursL2[k]));
            System.out.println("  Erreur max = " + String.format("%.6e", erreursMax[k]));
        }

        // Calcul des ordres de convergence
        double[] ordresL2 = new double[taillesN.length - 1];
        double[] ordresMax = new double[taillesN.length - 1];

        for (int k = 0; k < taillesN.length - 1; k++) {
            double h1 = taillesMaillage[k];
            double h2 = taillesMaillage[k + 1];
            double e1_L2 = erreursL2[k];
            double e2_L2 = erreursL2[k + 1];
            double e1_max = erreursMax[k];
            double e2_max = erreursMax[k + 1];

            // Calcul de l'ordre : p = log(e1/e2) / log(h1/h2)
            if (e2_L2 > 0 && e1_L2 > 0) {
                ordresL2[k] = Math.log(e1_L2 / e2_L2) / Math.log(h1 / h2);
            }
            if (e2_max > 0 && e1_max > 0) {
                ordresMax[k] = Math.log(e1_max / e2_max) / Math.log(h1 / h2);
            }

            System.out.println("Ordre de convergence entre N=" + taillesN[k] + " et N=" + taillesN[k+1] + ":");
            System.out.println("  Norme L²: " + String.format("%.2f", ordresL2[k]));
            System.out.println("  Norme max: " + String.format("%.2f", ordresMax[k]));
        }

        // Ordre de convergence moyen
        double ordreL2Moyen = calculerMoyenne(ordresL2);
        double ordreMaxMoyen = calculerMoyenne(ordresMax);

        System.out.println("\n=== Résultats de convergence ===");
        System.out.println("Ordre de convergence moyen:");
        System.out.println("  Norme L²: " + String.format("%.2f", ordreL2Moyen));
        System.out.println("  Norme max: " + String.format("%.2f", ordreMaxMoyen));
        System.out.println("Ordre théorique attendu: 2.0 (différences finies O(h²))");

        solveur.fermer();

        return new EtudeConvergence(taillesMaillage, erreursL2, erreursMax,
            ordreL2Moyen, ordreMaxMoyen, ordresL2);
    }

    /**
     * Analyse la convergence d'une méthode itérative
     *
     * @param historiqueErreurs Historique des erreurs par itération
     * @return Facteur de convergence estimé
     */
    public static double analyserConvergenceIterative(double[] historiqueErreurs) {
        if (historiqueErreurs.length < 3) {
            return Double.NaN;
        }

        // Calcul du facteur de convergence asymptotique
        // ρ ≈ e(k+1) / e(k) pour k suffisamment grand
        int debut = Math.max(1, historiqueErreurs.length / 2); // Prendre la seconde moitié
        double sommeRapports = 0.0;
        int compteur = 0;

        for (int k = debut; k < historiqueErreurs.length - 1; k++) {
            if (historiqueErreurs[k] > 1e-15 && historiqueErreurs[k+1] > 1e-15) {
                double rapport = historiqueErreurs[k+1] / historiqueErreurs[k];
                if (rapport > 0 && rapport < 1) { // Convergence
                    sommeRapports += rapport;
                    compteur++;
                }
            }
        }

        double facteurConvergence = (compteur > 0) ? sommeRapports / compteur : Double.NaN;

        System.out.println("Analyse de convergence itérative:");
        System.out.println("  Facteur de convergence ρ ≈ " + String.format("%.4f", facteurConvergence));
        System.out.println("  Taux de convergence: " + String.format("%.2f", -Math.log(facteurConvergence)));

        return facteurConvergence;
    }

    /**
     * Calcule l'erreur de discrétisation (différence entre solution exacte et discrète)
     * pour un maillage donné, indépendamment de la méthode de résolution
     *
     * @param maillage Le maillage avec la solution exacte
     * @return Erreur de discrétisation théorique
     */
    public static double calculerErreurDiscretisation(Maillage maillage) {
        // Pour les différences finies centrées d'ordre 2:
        // L'erreur de discrétisation est en O(h²)
        double h = maillage.getH();

        // Estimation basée sur la dérivée quatrième (si disponible)
        // Pour sin(πx)sin(πy): d⁴u/dx⁴ ≈ π⁴ sin(πx)sin(πy)
        double erreurTheorique = h * h * Math.PI * Math.PI / 12.0; // Approximation

        System.out.println("Erreur de discrétisation théorique ≈ " + String.format("%.6e", erreurTheorique));

        return erreurTheorique;
    }

    /**
     * Compare les performances de différentes méthodes
     *
     * @param maillage Le maillage de test
     * @param methodes Les méthodes à comparer
     * @return Résultats comparatifs
     */
    public static void comparerMethodes(Maillage maillage, SolveurGaussSeidel.MethodeResolution[] methodes) {
        System.out.println("\n=== Comparaison des méthodes ===");

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();

        for (SolveurGaussSeidel.MethodeResolution methode : methodes) {
            System.out.println("\nTest de: " + methode.getNom());

            // Sauvegarde de l'état initial
            double[][] solutionInitiale = maillage.copierSolution();

            long debut = System.currentTimeMillis();
            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);
            long fin = System.currentTimeMillis();

            System.out.println("  Temps: " + (fin - debut) + " ms");
            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Convergé: " + resultat.converge);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            // Calcul de l'erreur par rapport à la solution exacte
            ResultatAnalyse analyse = calculerErreurs(maillage);
            System.out.println("  Erreur L²: " + String.format("%.6e", analyse.erreurL2));

            // Analyse de la convergence
            double facteur = analyserConvergenceIterative(resultat.historiqueErreurs);
            System.out.println("  Facteur de convergence: " + String.format("%.4f", facteur));

            // Restauration pour le test suivant
            maillage.restaurerSolution(solutionInitiale);
        }

        solveur.fermer();
    }

    /**
     * Utilitaire pour calculer la moyenne d'un tableau
     */
    private static double calculerMoyenne(double[] valeurs) {
        double somme = 0.0;
        for (double valeur : valeurs) {
            if (!Double.isNaN(valeur)) {
                somme += valeur;
            }
        }
        return somme / valeurs.length;
    }

    /**
     * Génère un rapport détaillé d'analyse
     */
    public static String genererRapport(ResultatAnalyse analyse, EtudeConvergence convergence) {
        StringBuilder rapport = new StringBuilder();

        rapport.append("=== RAPPORT D'ANALYSE DES ERREURS ===\n\n");

        rapport.append("1. ANALYSE DES ERREURS:\n");
        rapport.append("   - Erreur L² (intégration numérique): ").append(String.format("%.6e", analyse.erreurL2)).append("\n");
        rapport.append("   - Erreur maximum: ").append(String.format("%.6e", analyse.erreurMax)).append("\n");
        rapport.append("   - Erreur moyenne: ").append(String.format("%.6e", analyse.erreurMoyenne)).append("\n");
        rapport.append("   - Points analysés: ").append(analyse.pointsAnalyses).append("\n\n");

        if (convergence != null) {
            rapport.append("2. ORDRE DE CONVERGENCE:\n");
            rapport.append("   - Ordre L²: ").append(String.format("%.2f", convergence.ordreConvergenceL2)).append("\n");
            rapport.append("   - Ordre max: ").append(String.format("%.2f", convergence.ordreConvergenceMax)).append("\n");
            rapport.append("   - Ordre théorique: 2.0 (différences finies)\n\n");

            rapport.append("3. DÉTAIL PAR MAILLAGE:\n");
            for (int i = 0; i < convergence.taillesMaillage.length; i++) {
                rapport.append("   h = ").append(String.format("%.6f", convergence.taillesMaillage[i]));
                rapport.append(", E_L2 = ").append(String.format("%.6e", convergence.erreursL2[i]));
                rapport.append(", E_max = ").append(String.format("%.6e", convergence.erreursMax[i])).append("\n");
            }
        }

        return rapport.toString();
    }
}
