/**
 * Classe pour l'analyse des erreurs et le calcul de l'ordre de convergence
 * Adaptée aux conditions aux limites de Dirichlet générales
 *
 * Cette classe implémente :
 * - Calcul de l'erreur en norme L² par intégration numérique
 * - Calcul de l'erreur en norme infinie (max)
 * - Détermination de l'ordre numérique de convergence
 * - Analyse de la convergence des méthodes itératives
 * - Prise en compte des conditions aux limites dans l'analyse
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
        public final String typeAnalyse;
        public final boolean solutionExacteDisponible;

        public ResultatAnalyse(double erreurL2, double erreurMax, double erreurMoyenne,
                               int pointsAnalyses, double[][] carteErreurs, String type, boolean exact) {
            this.erreurL2 = erreurL2;
            this.erreurMax = erreurMax;
            this.erreurMoyenne = erreurMoyenne;
            this.pointsAnalyses = pointsAnalyses;
            this.carteErreurs = carteErreurs;
            this.typeAnalyse = type;
            this.solutionExacteDisponible = exact;
        }
    }

    /**
     * Classe pour les résultats d'étude de convergence
     */
    public static class EtudeConvergence {
        public final double[] taillesMaillage;
        public final double[] erreursL2;
        public final double[] erreursMax;
        public final double[] erreursDiscretes; // Erreur de discrétisation
        public final double ordreConvergenceL2;
        public final double ordreConvergenceMax;
        public final double[] ordresLocaux;
        public final String[] conditionsLimites;

        public EtudeConvergence(double[] taillesMaillage, double[] erreursL2, double[] erreursMax,
                                double[] erreursDiscretes, double ordreConvergenceL2,
                                double ordreConvergenceMax, double[] ordresLocaux, String[] conditions) {
            this.taillesMaillage = taillesMaillage;
            this.erreursL2 = erreursL2;
            this.erreursMax = erreursMax;
            this.erreursDiscretes = erreursDiscretes;
            this.ordreConvergenceL2 = ordreConvergenceL2;
            this.ordreConvergenceMax = ordreConvergenceMax;
            this.ordresLocaux = ordresLocaux;
            this.conditionsLimites = conditions;
        }
    }

    /**
     * Calcule l'erreur entre la solution numérique et la solution exacte
     * Prend en compte les conditions aux limites générales
     *
     * L'erreur L² est calculée par intégration numérique sur le domaine intérieur :
     * ||e||₂ = √(∫∫ |U_numerical - U_exact|² dx dy)
     *
     * Pour la discrétisation avec CL générales, on utilise :
     * ||e||₂ ≈ √(h² * Σᵢⱼ |U[i,j] - U_exact[i,j]|²) pour (i,j) intérieurs
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

        System.out.println("Calcul des erreurs avec conditions aux limites: " +
            maillage.getConditionsLimites().getDescription());
        System.out.println("Pas du maillage h = " + h);

        // Vérification de la disponibilité de la solution exacte
        boolean solutionExacteValide = verifierSolutionExacte(exactSol, N);

        if (!solutionExacteValide) {
            System.out.println("Solution exacte non disponible - analyse limitée");
            return new ResultatAnalyse(0.0, 0.0, 0.0, 0, carteErreurs,
                "Aucune solution exacte", false);
        }

        // Parcours de tous les points (y compris les bords pour analyse complète)
        for (int i = 0; i <= N+1; i++) {
            for (int j = 0; j <= N+1; j++) {
                // Erreur locale au point (i,j)
                double erreurLocale = Math.abs(U[i][j] - exactSol[i][j]);
                carteErreurs[i][j] = erreurLocale;

                // Accumulation pour les différentes normes
                // Pour les points intérieurs uniquement (les CL sont exactes par construction)
                if (!maillage.isBoundary(i, j)) {
                    sommeErreursCarrees += erreurLocale * erreurLocale;
                    erreurMax = Math.max(erreurMax, erreurLocale);
                    sommeErreurs += erreurLocale;
                    compteurPoints++;
                } else {
                    // Vérification que les CL sont bien respectées
                    if (erreurLocale > 1e-12) {
                        System.err.println("Attention: Erreur sur CL au point (" + i + "," + j + "): " + erreurLocale);
                    }
                }
            }
        }

        // Calcul des normes avec intégration numérique
        double erreurL2 = Math.sqrt(h * h * sommeErreursCarrees);
        double erreurMoyenne = sommeErreurs / compteurPoints;

        System.out.println("Erreurs calculées sur " + compteurPoints + " points intérieurs:");
        System.out.println("  Norme L² (intégration numérique): " + String.format("%.6e", erreurL2));
        System.out.println("  Norme max: " + String.format("%.6e", erreurMax));
        System.out.println("  Erreur moyenne: " + String.format("%.6e", erreurMoyenne));

        return new ResultatAnalyse(erreurL2, erreurMax, erreurMoyenne, compteurPoints,
            carteErreurs, "Analyse avec CL générales", true);
    }

    /**
     * Calcule l'erreur de discrétisation théorique
     * Pour les différences finies avec CL de Dirichlet générales
     */
    public static double calculerErreurDiscretisation(Maillage maillage) {
        double h = maillage.getH();

        // Pour les différences finies centrées d'ordre 2 avec CL de Dirichlet:
        // L'erreur de discrétisation est en O(h²) dans la norme L²
        // L'erreur dépend de la régularité de la solution exacte et des CL

        // Estimation conservative pour CL générales
        double erreurTheorique = h * h / 12.0; // Coefficient dépendant du problème

        System.out.println("Erreur de discrétisation théorique ≈ " + String.format("%.6e", erreurTheorique));
        System.out.println("Note: Estimation pour CL de Dirichlet générales");

        return erreurTheorique;
    }

    /**
     * Effectue une étude de convergence avec conditions aux limites fixées
     */
    public static EtudeConvergence etudierConvergence(Maillage.CasTest casTest, int[] taillesN,
                                                      SolveurGaussSeidel.MethodeResolution methode,
                                                      ConditionsLimites conditionsFixees) {

        System.out.println("=== Étude de convergence avec CL générales ===");
        System.out.println("Cas de test: " + casTest.getDescription());
        System.out.println("Méthode: " + methode.getNom());
        System.out.println("Conditions: " + conditionsFixees.getDescription());
        System.out.println("Tailles de maillage: " + java.util.Arrays.toString(taillesN));

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();

        double[] taillesMaillage = new double[taillesN.length];
        double[] erreursL2 = new double[taillesN.length];
        double[] erreursMax = new double[taillesN.length];
        double[] erreursDiscretes = new double[taillesN.length];
        String[] descriptionsConditions = new String[taillesN.length];

        // Résolution pour chaque taille de maillage
        for (int k = 0; k < taillesN.length; k++) {
            int N = taillesN[k];
            System.out.println("\nRésolution pour N = " + N + " (maillage " + (N+2) + "×" + (N+2) + ")");

            // Création du maillage avec conditions fixées
            Maillage maillage = new Maillage(N, conditionsFixees);
            maillage.configurerCasTest(casTest, conditionsFixees);

            taillesMaillage[k] = maillage.getH();
            descriptionsConditions[k] = conditionsFixees.getDescription();

            // Calcul de l'erreur de discrétisation théorique
            erreursDiscretes[k] = calculerErreurDiscretisation(maillage);

            // Résolution numérique
            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);

            if (!resultat.converge) {
                System.err.println("Attention: Non convergence pour N = " + N);
            }

            // Calcul des erreurs par rapport à la solution exacte
            ResultatAnalyse analyse = calculerErreurs(maillage);

            if (analyse.solutionExacteDisponible) {
                erreursL2[k] = analyse.erreurL2;
                erreursMax[k] = analyse.erreurMax;
            } else {
                // Si pas de solution exacte, utiliser des métriques alternatives
                erreursL2[k] = resultat.erreurFinale;
                erreursMax[k] = resultat.erreurFinale;
                System.out.println("Utilisation de l'erreur itérative comme métrique");
            }

            System.out.println("  h = " + String.format("%.6f", taillesMaillage[k]));
            System.out.println("  Erreur L² = " + String.format("%.6e", erreursL2[k]));
            System.out.println("  Erreur max = " + String.format("%.6e", erreursMax[k]));
            System.out.println("  Erreur discrète = " + String.format("%.6e", erreursDiscretes[k]));
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
            } else {
                ordresL2[k] = Double.NaN;
            }

            if (e2_max > 0 && e1_max > 0) {
                ordresMax[k] = Math.log(e1_max / e2_max) / Math.log(h1 / h2);
            } else {
                ordresMax[k] = Double.NaN;
            }

            System.out.println("Ordre de convergence entre N=" + taillesN[k] + " et N=" + taillesN[k+1] + ":");
            System.out.println("  Norme L²: " + String.format("%.2f", ordresL2[k]));
            System.out.println("  Norme max: " + String.format("%.2f", ordresMax[k]));
        }

        // Ordre de convergence moyen
        double ordreL2Moyen = calculerMoyenneSansNaN(ordresL2);
        double ordreMaxMoyen = calculerMoyenneSansNaN(ordresMax);

        System.out.println("\n=== Résultats de convergence ===");
        System.out.println("Ordre de convergence moyen:");
        System.out.println("  Norme L²: " + String.format("%.2f", ordreL2Moyen));
        System.out.println("  Norme max: " + String.format("%.2f", ordreMaxMoyen));
        System.out.println("Ordre théorique attendu: 2.0 (différences finies O(h²))");
        System.out.println("Conditions aux limites: " + conditionsFixees.getDescription());

        solveur.fermer();

        return new EtudeConvergence(taillesMaillage, erreursL2, erreursMax, erreursDiscretes,
            ordreL2Moyen, ordreMaxMoyen, ordresL2, descriptionsConditions);
    }

    /**
     * Surcharge pour utiliser les conditions recommandées du cas de test
     */
    public static EtudeConvergence etudierConvergence(Maillage.CasTest casTest, int[] taillesN,
                                                      SolveurGaussSeidel.MethodeResolution methode) {

        ConditionsLimites conditionsDefaut = ConditionsLimites.creerConditionsTest(
            casTest.getConditionsRecommandees());

        return etudierConvergence(casTest, taillesN, methode, conditionsDefaut);
    }

    /**
     * Analyse l'influence des conditions aux limites sur la convergence
     */
    public static void analyserInfluenceConditionsLimites(Maillage.CasTest casTest, int N,
                                                          SolveurGaussSeidel.MethodeResolution methode) {

        System.out.println("=== Analyse de l'influence des conditions aux limites ===");
        System.out.println("Cas: " + casTest.getDescription());
        System.out.println("Maillage: " + (N+2) + "×" + (N+2));

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();

        // Test avec différents types de conditions
        String[] typesConditions = {"homogenes", "constantes_unitaires", "constantes_variables",
            "lineaires", "sinusoidales"};

        for (String type : typesConditions) {
            System.out.println("\n--- Conditions: " + type + " ---");

            ConditionsLimites conditions = ConditionsLimites.creerConditionsTest(type);
            Maillage maillage = new Maillage(N, conditions);
            maillage.configurerCasTest(casTest, conditions);

            long debut = System.currentTimeMillis();
            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);
            long fin = System.currentTimeMillis();

            System.out.println("  Temps: " + (fin - debut) + " ms");
            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Convergé: " + resultat.converge);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            // Analyse de la solution
            ResultatAnalyse analyse = calculerErreurs(maillage);
            if (analyse.solutionExacteDisponible) {
                System.out.println("  Erreur L²: " + String.format("%.6e", analyse.erreurL2));
            }

            // Calcul du résidu
            double residu = calculerResidu(maillage);
            System.out.println("  Résidu: " + String.format("%.2e", residu));

            // Statistiques sur la solution
            analyserDistributionSolution(maillage);
        }

        solveur.fermer();
    }

    /**
     * Calcule le résidu de l'équation discrétisée
     */
    public static double calculerResidu(Maillage maillage) {
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
                    // Calcul du résidu local: r = |f + ΔU|
                    double laplacienDiscret = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] - 4*U[i][j]) / h2;
                    double residu = Math.abs(F[i][j] + laplacienDiscret);

                    residuMax = Math.max(residuMax, residu);
                    residuL2 += residu * residu;
                    compteur++;
                }
            }
        }

        residuL2 = Math.sqrt(residuL2 / compteur);

        return residuMax;
    }

    /**
     * Analyse la distribution de la solution
     */
    private static void analyserDistributionSolution(Maillage maillage) {
        double[][] U = maillage.getU();
        int N = maillage.getN();

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        double somme = 0.0, sommeCarres = 0.0;
        int compteur = 0;

        // Statistiques sur le domaine intérieur
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                double val = U[i][j];
                min = Math.min(min, val);
                max = Math.max(max, val);
                somme += val;
                sommeCarres += val * val;
                compteur++;
            }
        }

        double moyenne = somme / compteur;
        double variance = (sommeCarres / compteur) - (moyenne * moyenne);
        double ecartType = Math.sqrt(variance);

        System.out.println("  Distribution: min=" + String.format("%.3f", min) +
            ", max=" + String.format("%.3f", max) +
            ", moy=" + String.format("%.3f", moyenne) +
            ", σ=" + String.format("%.3f", ecartType));
    }

    /**
     * Analyse la convergence d'une méthode itérative
     */
    public static double analyserConvergenceIterative(double[] historiqueErreurs) {
        if (historiqueErreurs.length < 3) {
            return Double.NaN;
        }

        // Calcul du facteur de convergence asymptotique
        int debut = Math.max(1, historiqueErreurs.length / 2);
        double sommeRapports = 0.0;
        int compteur = 0;

        for (int k = debut; k < historiqueErreurs.length - 1; k++) {
            if (historiqueErreurs[k] > 1e-15 && historiqueErreurs[k+1] > 1e-15) {
                double rapport = historiqueErreurs[k+1] / historiqueErreurs[k];
                if (rapport > 0 && rapport < 1) {
                    sommeRapports += rapport;
                    compteur++;
                }
            }
        }

        double facteurConvergence = (compteur > 0) ? sommeRapports / compteur : Double.NaN;

        if (!Double.isNaN(facteurConvergence)) {
            System.out.println("Analyse de convergence itérative:");
            System.out.println("  Facteur de convergence ρ ≈ " + String.format("%.4f", facteurConvergence));
            System.out.println("  Taux de convergence: " + String.format("%.2f", -Math.log(facteurConvergence)));
        }

        return facteurConvergence;
    }

    /**
     * Compare les performances avec différentes conditions aux limites
     */
    public static void comparerMethodesAvecConditions(Maillage.CasTest casTest, int N) {
        System.out.println("\n=== Comparaison méthodes avec conditions générales ===");

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();
        SolveurGaussSeidel.MethodeResolution[] methodes = {
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION,
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_PARALLELE
        };

        // Test avec conditions non homogènes
        ConditionsLimites conditions = ConditionsLimites.creerConditionsTest("constantes_variables");

        for (SolveurGaussSeidel.MethodeResolution methode : methodes) {
            System.out.println("\n--- " + methode.getNom() + " ---");

            Maillage maillage = new Maillage(N, conditions);
            maillage.configurerCasTest(casTest, conditions);

            double omega = (methode == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                SolveurGaussSeidel.calculerOmegaOptimal(N) : 1.0;

            long debut = System.currentTimeMillis();
            SolveurGaussSeidel.ResultatConvergence resultat =
                solveur.resoudre(maillage, methode, 1e-6, 5000, omega, null);
            long fin = System.currentTimeMillis();

            System.out.println("  Temps: " + (fin - debut) + " ms");
            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Convergé: " + resultat.converge);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            // Analyse spécifique aux CL
            double residu = calculerResidu(maillage);
            System.out.println("  Résidu: " + String.format("%.2e", residu));

            if (resultat.converge) {
                double facteur = analyserConvergenceIterative(resultat.historiqueErreurs);
                if (!Double.isNaN(facteur)) {
                    System.out.println("  Facteur convergence: " + String.format("%.4f", facteur));
                }
            }
        }

        solveur.fermer();
    }

    /**
     * Vérifie si une solution exacte est disponible et valide
     */
    private static boolean verifierSolutionExacte(double[][] exactSol, int N) {
        boolean nonNulle = false;
        for (int i = 0; i <= N+1 && !nonNulle; i++) {
            for (int j = 0; j <= N+1 && !nonNulle; j++) {
                if (Math.abs(exactSol[i][j]) > 1e-12) {
                    nonNulle = true;
                }
            }
        }
        return nonNulle;
    }

    /**
     * Calcule la moyenne en ignorant les valeurs NaN
     */
    private static double calculerMoyenneSansNaN(double[] valeurs) {
        double somme = 0.0;
        int compteur = 0;
        for (double valeur : valeurs) {
            if (!Double.isNaN(valeur)) {
                somme += valeur;
                compteur++;
            }
        }
        return (compteur > 0) ? somme / compteur : Double.NaN;
    }

    /**
     * Effectue une analyse de sensibilité aux conditions aux limites
     */
    public static void analyseSensibiliteConditions(Maillage.CasTest casTest, int N) {
        System.out.println("=== Analyse de sensibilité aux conditions aux limites ===");

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();

        // Test avec des perturbations des conditions aux limites
        double[] perturbations = {0.0, 0.1, 0.5, 1.0};

        for (double perturbation : perturbations) {
            System.out.println("\nPerturbation: " + perturbation);

            // Création de conditions perturbées
            ConditionsLimites conditions = new ConditionsLimites(
                perturbation, perturbation, perturbation, perturbation);

            Maillage maillage = new Maillage(N, conditions);
            maillage.configurerCasTest(casTest, conditions);

            SolveurGaussSeidel.ResultatConvergence resultat =
                solveur.resoudre(maillage, SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE);

            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale));

            // Analyse de la norme de la solution
            analyserNormeSolution(maillage);
        }

        solveur.fermer();
    }

    /**
     * Calcule différentes normes de la solution
     */
    private static void analyserNormeSolution(Maillage maillage) {
        double[][] U = maillage.getU();
        int N = maillage.getN();
        double h = maillage.getH();

        double normeL2 = 0.0;
        double normeMax = 0.0;
        double normeL1 = 0.0;

        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                double val = Math.abs(U[i][j]);
                normeL2 += val * val;
                normeMax = Math.max(normeMax, val);
                normeL1 += val;
            }
        }

        normeL2 = h * Math.sqrt(normeL2);
        normeL1 = h * h * normeL1;

        System.out.println("  Normes solution: L1=" + String.format("%.3f", normeL1) +
            ", L2=" + String.format("%.3f", normeL2) +
            ", L∞=" + String.format("%.3f", normeMax));
    }

    /**
     * Génère un rapport détaillé d'analyse avec conditions aux limites
     */
    public static String genererRapportComplet(ResultatAnalyse analyse, EtudeConvergence convergence,
                                               Maillage maillage) {
        StringBuilder rapport = new StringBuilder();

        rapport.append("=== RAPPORT D'ANALYSE AVEC CONDITIONS GÉNÉRALES ===\n\n");

        // Informations sur les conditions aux limites
        rapport.append("CONDITIONS AUX LIMITES:\n");
        rapport.append("   Type: ").append(maillage.getConditionsLimites().getDescription()).append("\n");
        rapport.append("   Compatibilité: ").append(maillage.getConditionsLimites().verifierCompatibilite() ? "OK" : "Problème").append("\n\n");

        // Analyse des erreurs
        rapport.append("ANALYSE DES ERREURS:\n");
        rapport.append("   Type d'analyse: ").append(analyse.typeAnalyse).append("\n");
        rapport.append("   Solution exacte: ").append(analyse.solutionExacteDisponible ? "Disponible" : "Non disponible").append("\n");

        if (analyse.solutionExacteDisponible) {
            rapport.append("   Erreur L² (intégration numérique): ").append(String.format("%.6e", analyse.erreurL2)).append("\n");
            rapport.append("   Erreur maximum: ").append(String.format("%.6e", analyse.erreurMax)).append("\n");
            rapport.append("   Erreur moyenne: ").append(String.format("%.6e", analyse.erreurMoyenne)).append("\n");
        }
        rapport.append("   Points analysés: ").append(analyse.pointsAnalyses).append("\n\n");

        if (convergence != null) {
            rapport.append("ORDRE DE CONVERGENCE:\n");
            rapport.append("   Ordre L²: ").append(String.format("%.2f", convergence.ordreConvergenceL2)).append("\n");
            rapport.append("   Ordre max: ").append(String.format("%.2f", convergence.ordreConvergenceMax)).append("\n");
            rapport.append("   Ordre théorique: 2.0 (différences finies avec CL de Dirichlet)\n\n");

            rapport.append("DÉTAIL PAR MAILLAGE:\n");
            for (int i = 0; i < convergence.taillesMaillage.length; i++) {
                rapport.append("   h = ").append(String.format("%.6f", convergence.taillesMaillage[i]));
                rapport.append(", E_L2 = ").append(String.format("%.6e", convergence.erreursL2[i]));
                rapport.append(", E_max = ").append(String.format("%.6e", convergence.erreursMax[i]));
                rapport.append(", E_disc = ").append(String.format("%.6e", convergence.erreursDiscretes[i])).append("\n");
            }
        }

        // Vérifications de cohérence
        rapport.append("\nVÉRIFICATIONS:\n");
        rapport.append("   Cohérence maillage: ").append(maillage.verifierCoherence() ? "OK" : "Problème").append("\n");
        double residu = calculerResidu(maillage);
        rapport.append("   Résidu équation: ").append(String.format("%.2e", residu)).append("\n");

        return rapport.toString();
    }

    /**
     * Méthode utilitaire pour l'analyse rapide
     */
    public static void analyseRapide(Maillage maillage, SolveurGaussSeidel.ResultatConvergence resultat) {
        System.out.println("=== Analyse rapide ===");

        ResultatAnalyse analyse = calculerErreurs(maillage);
        double residu = calculerResidu(maillage);

        System.out.println("Résolution: " + (resultat.converge ? "Convergée" : "Non convergée"));
        System.out.println("Itérations: " + resultat.iterations);
        System.out.println("Résidu: " + String.format("%.2e", residu));

        if (analyse.solutionExacteDisponible) {
            System.out.println("Erreur L²: " + String.format("%.6e", analyse.erreurL2));
            System.out.println("Erreur max: " + String.format("%.6e", analyse.erreurMax));
        }

        analyserDistributionSolution(maillage);
        System.out.println("======================");
    }
}
