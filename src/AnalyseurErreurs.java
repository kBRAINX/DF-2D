import java.util.Arrays; // Ajout pour Arrays.stream et Arrays.toString

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
 *
 * N_total est le nombre total de points. Les erreurs sont calculées sur les N_interieur = N_total-2 points intérieurs.
 */
public class AnalyseurErreurs {

    /**
     * Classe pour stocker les résultats d'analyse d'erreur
     */
    public static class ResultatAnalyse {
        public final double erreurL2;
        public final double erreurMax;
        public final double erreurMoyenne;
        public final int pointsAnalyses; // Nombre de points intérieurs
        public final double[][] carteErreurs; // Taille N_total x N_total
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
        public final double[] pasMaillageH; // Tableau des valeurs de h
        public final int[] taillesN_total;   // Tableau des N_total correspondants
        public final double[] erreursL2;
        public final double[] erreursMax;
        public final double[] erreursDiscretes; // Erreur de discrétisation
        public final double ordreConvergenceL2;
        public final double ordreConvergenceMax;
        public final double[] ordresLocauxL2; // Ordres entre chaque paire de maillages
        public final double[] ordresLocauxMax;
        public final String[] conditionsLimites; // Description des CL pour chaque maillage (devrait être la même)


        public EtudeConvergence(double[] pasMaillageH, int[] taillesN_total,
                                double[] erreursL2, double[] erreursMax,
                                double[] erreursDiscretes, double ordreConvergenceL2,
                                double ordreConvergenceMax, double[] ordresLocauxL2, double[] ordresLocauxMax,
                                String[] conditions) {
            this.pasMaillageH = pasMaillageH;
            this.taillesN_total = taillesN_total;
            this.erreursL2 = erreursL2;
            this.erreursMax = erreursMax;
            this.erreursDiscretes = erreursDiscretes;
            this.ordreConvergenceL2 = ordreConvergenceL2;
            this.ordreConvergenceMax = ordreConvergenceMax;
            this.ordresLocauxL2 = ordresLocauxL2;
            this.ordresLocauxMax = ordresLocauxMax;
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
        int N_total = maillage.getN_total();
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();

        // Carte des erreurs locales, taille N_total x N_total
        double[][] carteErreurs = new double[N_total][N_total];

        double sommeErreursCarrees = 0.0;
        double erreurMax = 0.0;
        double sommeErreurs = 0.0;
        int compteurPointsInterieurs = 0;

        System.out.println("Calcul des erreurs avec conditions aux limites: " +
            maillage.getConditionsLimites().getDescription());
        System.out.println("Pas du maillage h = " + h + ", N_total=" + N_total + ", N_interieur=" + N_interieur);

        boolean solutionExacteValide = verifierSolutionExacte(exactSol, N_total);

        if (!solutionExacteValide) {
            System.out.println("Solution exacte non disponible ou nulle - analyse d'erreur limitée.");
            return new ResultatAnalyse(Double.NaN, Double.NaN, Double.NaN, 0, carteErreurs,
                "Aucune solution exacte valide", false);
        }

        // Parcours de tous les points (y compris les bords pour la carteErreurs)
        for (int i = 0; i < N_total; i++) { // Indice global i
            for (int j = 0; j < N_total; j++) { // Indice global j
                double erreurLocale = Math.abs(U[i][j] - exactSol[i][j]);
                carteErreurs[i][j] = erreurLocale;

                // Accumulation pour les normes sur les points INTÉRIEURS uniquement
                if (!maillage.isBoundary(i, j)) { // ou i > 0 && i < N_total-1 && j > 0 && j < N_total-1
                    sommeErreursCarrees += erreurLocale * erreurLocale;
                    erreurMax = Math.max(erreurMax, erreurLocale);
                    sommeErreurs += erreurLocale;
                    compteurPointsInterieurs++;
                } else { // Points sur le bord
                    if (erreurLocale > 1e-12 && solutionExacteValide) { // Tolérance pour erreurs CL
                        // Si la solution exacte est bien celle qui satisfait les CL, cette erreur devrait être nulle.
                        // System.err.println("Attention: Erreur sur CL au point (" + i + "," + j + "): " + erreurLocale);
                    }
                }
            }
        }

        if (compteurPointsInterieurs == 0) {
            System.out.println("Aucun point intérieur pour calculer les normes d'erreur.");
            return new ResultatAnalyse(0.0, 0.0, 0.0, 0, carteErreurs,
                "Analyse (pas de points intérieurs)", solutionExacteValide);
        }

        // Calcul des normes avec intégration numérique (sommation discrète)
        double erreurL2 = Math.sqrt(h * h * sommeErreursCarrees); // Approche par somme de Riemann
        double erreurMoyenne = sommeErreurs / compteurPointsInterieurs;

        System.out.println("Erreurs calculées sur " + compteurPointsInterieurs + " points intérieurs:");
        System.out.println("  Norme L² (intégration numérique): " + String.format("%.6e", erreurL2));
        System.out.println("  Norme max: " + String.format("%.6e", erreurMax));
        System.out.println("  Erreur moyenne: " + String.format("%.6e", erreurMoyenne));

        return new ResultatAnalyse(erreurL2, erreurMax, erreurMoyenne, compteurPointsInterieurs,
            carteErreurs, "Analyse avec CL générales", true);
    }

    /**
     * Calcule l'erreur de discrétisation théorique (estimation)
     * Pour les différences finies avec CL de Dirichlet générales
     */
    public static double calculerErreurDiscretisation(Maillage maillage) {
        double h = maillage.getH();
        // Pour les différences finies centrées d'ordre 2 avec CL de Dirichlet:
        // L'erreur de discrétisation est en O(h²)
        // Une estimation grossière est C * h^2. On prend C=1/12 (lié à la dérivée quatrième).
        double erreurTheorique = h * h / 12.0;
        // System.out.println("Estimation erreur de discrétisation théorique ≈ " + String.format("%.6e", erreurTheorique));
        return erreurTheorique;
    }

    /**
     * Effectue une étude de convergence avec conditions aux limites fixées
     * @param casTest Le cas de test
     * @param taillesN_total Tableau des N_total (nombre total de points) à tester
     * @param methode Méthode de résolution
     * @param conditionsFixees Conditions aux limites à utiliser pour tous les maillages
     * @return Resultats de l'étude
     */
    public static EtudeConvergence etudierConvergence(Maillage.CasTest casTest, int[] taillesN_total,
                                                      SolveurGaussSeidel.MethodeResolution methode,
                                                      ConditionsLimites conditionsFixees) {
        System.out.println("=== Étude de convergence avec CL générales ===");
        System.out.println("Cas de test: " + casTest.getDescription());
        System.out.println("Méthode: " + methode.getNom());
        System.out.println("Conditions: " + conditionsFixees.getDescription());
        System.out.println("Tailles N_total (bords inclus): " + Arrays.toString(taillesN_total));

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();
        int nbMaillages = taillesN_total.length;

        double[] pasMaillageH = new double[nbMaillages];
        double[] erreursL2 = new double[nbMaillages];
        double[] erreursMax = new double[nbMaillages];
        double[] erreursDiscretes = new double[nbMaillages];
        String[] descriptionsConditions = new String[nbMaillages]; // Devrait être la même

        for (int k = 0; k < nbMaillages; k++) {
            int N_total_courant = taillesN_total[k];
            System.out.println("\nRésolution pour N_total = " + N_total_courant + " (maillage " + N_total_courant + "×" + N_total_courant + ")");

            Maillage maillage = new Maillage(N_total_courant, conditionsFixees);
            maillage.configurerCasTest(casTest, conditionsFixees); // Re-config cas test avec les CL

            pasMaillageH[k] = maillage.getH();
            descriptionsConditions[k] = conditionsFixees.getDescription(); // Stocker pour info
            erreursDiscretes[k] = calculerErreurDiscretisation(maillage);

            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);
            if (!resultat.converge) {
                System.err.println("Attention: Non convergence pour N_total = " + N_total_courant);
            }

            ResultatAnalyse analyse = calculerErreurs(maillage);
            if (analyse.solutionExacteDisponible) {
                erreursL2[k] = analyse.erreurL2;
                erreursMax[k] = analyse.erreurMax;
            } else {
                erreursL2[k] = Double.NaN; // Ou utiliser l'erreur itérative
                erreursMax[k] = Double.NaN;
                System.out.println("Solution exacte non disponible, impossible de calculer l'erreur de discrétisation.");
            }
            System.out.println("  h = " + String.format("%.6f", pasMaillageH[k]));
            System.out.println("  Erreur L² = " + String.format("%.6e", erreursL2[k]));
            System.out.println("  Erreur max = " + String.format("%.6e", erreursMax[k]));
        }

        double[] ordresLocauxL2 = new double[nbMaillages - 1];
        double[] ordresLocauxMax = new double[nbMaillages - 1];

        for (int k = 0; k < nbMaillages - 1; k++) {
            double h1 = pasMaillageH[k];
            double h2 = pasMaillageH[k+1];
            double e1_L2 = erreursL2[k];
            double e2_L2 = erreursL2[k+1];
            double e1_max = erreursMax[k];
            double e2_max = erreursMax[k+1];

            if (!Double.isNaN(e1_L2) && !Double.isNaN(e2_L2) && e2_L2 > 1e-15 && e1_L2 > 1e-15) {
                ordresLocauxL2[k] = Math.log(e1_L2 / e2_L2) / Math.log(h1 / h2);
            } else {
                ordresLocauxL2[k] = Double.NaN;
            }
            if (!Double.isNaN(e1_max) && !Double.isNaN(e2_max) && e2_max > 1e-15 && e1_max > 1e-15) {
                ordresLocauxMax[k] = Math.log(e1_max / e2_max) / Math.log(h1 / h2);
            } else {
                ordresLocauxMax[k] = Double.NaN;
            }
            System.out.println("Ordre de convergence (local) entre N_total=" + taillesN_total[k] + " et N_total=" + taillesN_total[k+1] + ":");
            System.out.println("  L²: " + String.format("%.2f", ordresLocauxL2[k]) + ", Max: " + String.format("%.2f", ordresLocauxMax[k]));
        }

        double ordreL2Moyen = calculerMoyenneSansNaN(ordresLocauxL2);
        double ordreMaxMoyen = calculerMoyenneSansNaN(ordresLocauxMax);

        System.out.println("\n=== Résultats de convergence ===");
        System.out.println("Ordre de convergence moyen (basé sur les ordres locaux):");
        System.out.println("  Norme L²: " + String.format("%.2f", ordreL2Moyen));
        System.out.println("  Norme max: " + String.format("%.2f", ordreMaxMoyen));
        System.out.println("Ordre théorique attendu: 2.0 (différences finies O(h²))");

        solveur.fermer();
        return new EtudeConvergence(pasMaillageH, taillesN_total, erreursL2, erreursMax, erreursDiscretes,
            ordreL2Moyen, ordreMaxMoyen, ordresLocauxL2, ordresLocauxMax, descriptionsConditions);
    }


    /**
     * Surcharge pour utiliser les conditions recommandées du cas de test
     */
    public static EtudeConvergence etudierConvergence(Maillage.CasTest casTest, int[] taillesN_total,
                                                      SolveurGaussSeidel.MethodeResolution methode) {
        ConditionsLimites conditionsDefaut = ConditionsLimites.creerConditionsTest(
            casTest.getConditionsRecommandees());
        return etudierConvergence(casTest, taillesN_total, methode, conditionsDefaut);
    }

    /**
     * Analyse l'influence des conditions aux limites sur la convergence
     * @param casTest Cas de test
     * @param N_total Nombre total de points pour le maillage
     * @param methode Méthode de résolution
     */
    public static void analyserInfluenceConditionsLimites(Maillage.CasTest casTest, int N_total,
                                                          SolveurGaussSeidel.MethodeResolution methode) {
        System.out.println("=== Analyse de l'influence des conditions aux limites ===");
        System.out.println("Cas: " + casTest.getDescription());
        System.out.println("Maillage N_total: " + N_total + "×" + N_total);

        SolveurGaussSeidel solveur = new SolveurGaussSeidel();
        String[] typesConditions = {"homogenes", "constantes_unitaires", "constantes_variables",
            "lineaires", "sinusoidales"};

        for (String type : typesConditions) {
            System.out.println("\n--- Conditions: " + type + " ---");
            ConditionsLimites conditions = ConditionsLimites.creerConditionsTest(type);
            Maillage maillage = new Maillage(N_total, conditions);
            maillage.configurerCasTest(casTest, conditions); // Important

            long debut = System.currentTimeMillis();
            SolveurGaussSeidel.ResultatConvergence resultat = solveur.resoudre(maillage, methode);
            long fin = System.currentTimeMillis();

            System.out.println("  Temps: " + (fin - debut) + " ms");
            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Convergé: " + resultat.converge);
            System.out.println("  Erreur finale (itérative): " + String.format("%.2e", resultat.erreurFinale));

            ResultatAnalyse analyseErreur = calculerErreurs(maillage);
            if (analyseErreur.solutionExacteDisponible) {
                System.out.println("  Erreur L² (discrétisation): " + String.format("%.6e", analyseErreur.erreurL2));
            }
            System.out.println("  Résidu (équation): " + String.format("%.2e", calculerResidu(maillage)));
            analyserDistributionSolution(maillage);
        }
        solveur.fermer();
    }


    /**
     * Calcule le résidu de l'équation discrétisée pour les points intérieurs
     */
    public static double calculerResidu(Maillage maillage) {
        double[][] U = maillage.getU();
        double[][] F = maillage.getF();
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();
        double h2 = h * h;

        if (N_interieur == 0) return 0.0;

        double residuMax = 0.0;
        double residuL2 = 0.0;
        int compteur = 0;

        // Boucles sur les indices GLOBAUX des points intérieurs
        for (int i = 1; i <= N_interieur; i++) {
            for (int j = 1; j <= N_interieur; j++) {
                double laplacienDiscret = (U[i-1][j] + U[i+1][j] + U[i][j-1] + U[i][j+1] - 4*U[i][j]) / h2;
                double residu = Math.abs(F[i][j] + laplacienDiscret); // F = -DeltaU => F + DeltaU = 0
                residuMax = Math.max(residuMax, residu);
                residuL2 += residu * residu;
                compteur++;
            }
        }
        return (compteur > 0) ? Math.sqrt(residuL2 / compteur) : 0.0; // RMS du résidu
    }

    /**
     * Analyse la distribution de la solution (sur les points intérieurs)
     */
    private static void analyserDistributionSolution(Maillage maillage) {
        double[][] U = maillage.getU();
        int N_interieur = maillage.getN_interieur();
        if (N_interieur == 0) {
            System.out.println("  Distribution: Pas de points intérieurs.");
            return;
        }

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        double somme = 0.0, sommeCarres = 0.0;

        for (int i = 1; i <= N_interieur; i++) { // Indices globaux des points intérieurs
            for (int j = 1; j <= N_interieur; j++) {
                double val = U[i][j];
                min = Math.min(min, val);
                max = Math.max(max, val);
                somme += val;
                sommeCarres += val * val;
            }
        }
        int compteur = N_interieur * N_interieur;
        double moyenne = somme / compteur;
        double variance = (sommeCarres / compteur) - (moyenne * moyenne);
        double ecartType = Math.sqrt(Math.max(0, variance)); // variance peut être négative à cause d'erreurs d'arrondi

        System.out.println("  Distribution (intérieure): min=" + String.format("%.3f", min) +
            ", max=" + String.format("%.3f", max) +
            ", moy=" + String.format("%.3f", moyenne) +
            ", σ=" + String.format("%.3f", ecartType));
    }


    /**
     * Analyse la convergence d'une méthode itérative à partir de l'historique des erreurs
     */
    public static double analyserConvergenceIterative(double[] historiqueErreurs) {
        if (historiqueErreurs == null || historiqueErreurs.length < 3) {
            // System.out.println("Historique d'erreurs insuffisant pour analyse de convergence itérative.");
            return Double.NaN;
        }

        int debut = Math.max(1, historiqueErreurs.length / 2); // Commencer à la moitié des itérations
        double sommeRapports = 0.0;
        int compteurValide = 0;

        for (int k = debut; k < historiqueErreurs.length - 1; k++) {
            if (historiqueErreurs[k] > 1e-15 && historiqueErreurs[k+1] > 1e-15) { // Éviter division par zéro ou erreurs
                double rapport = historiqueErreurs[k+1] / historiqueErreurs[k];
                if (rapport > 0 && rapport < 1.0) { // Uniquement si convergence et pas de divergence/stagnation exacte
                    sommeRapports += rapport;
                    compteurValide++;
                }
            }
        }

        double facteurConvergence = (compteurValide > 0) ? sommeRapports / compteurValide : Double.NaN;
        // System.out.println("Analyse de convergence itérative:");
        // System.out.println("  Facteur de convergence ρ ≈ " + String.format("%.4f", facteurConvergence));
        // if (!Double.isNaN(facteurConvergence)) {
        //     System.out.println("  Taux de convergence -ln(ρ) ≈ " + String.format("%.4f", -Math.log(facteurConvergence)));
        // }
        return facteurConvergence;
    }


    /**
     * Compare les performances avec différentes conditions aux limites
     * @param casTest Cas de test
     * @param N_total Nombre total de points
     */
    public static void comparerMethodesAvecConditions(Maillage.CasTest casTest, int N_total) {
        System.out.println("\n=== Comparaison méthodes avec conditions générales (N_total=" + N_total + ") ===");
        SolveurGaussSeidel solveur = new SolveurGaussSeidel();
        SolveurGaussSeidel.MethodeResolution[] methodes = {
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION,
            SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_PARALLELE
        };
        ConditionsLimites conditions = ConditionsLimites.creerConditionsTest("constantes_variables");

        for (SolveurGaussSeidel.MethodeResolution methode : methodes) {
            System.out.println("\n--- " + methode.getNom() + " ---");
            Maillage maillage = new Maillage(N_total, conditions);
            maillage.configurerCasTest(casTest, conditions);

            double omega = (methode == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                SolveurGaussSeidel.calculerOmegaOptimal(N_total) : 1.0;

            SolveurGaussSeidel.ResultatConvergence resultat =
                solveur.resoudre(maillage, methode, 1e-6, 5000, omega, null);

            System.out.println("  Temps: " + resultat.tempsCalcul + " ms");
            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Convergé: " + resultat.converge);
            System.out.println("  Erreur finale (itérative): " + String.format("%.2e", resultat.erreurFinale));
            System.out.println("  Résidu (équation): " + String.format("%.2e", calculerResidu(maillage)));
            if (resultat.converge) {
                double facteur = analyserConvergenceIterative(resultat.historiqueErreurs);
                if (!Double.isNaN(facteur)) {
                    System.out.println("  Facteur convergence (itératif): " + String.format("%.4f", facteur));
                }
            }
        }
        solveur.fermer();
    }


    /**
     * Vérifie si une solution exacte est disponible et valide (non nulle)
     */
    private static boolean verifierSolutionExacte(double[][] exactSol, int N_total) {
        if (exactSol == null) return false;
        boolean nonNulle = false;
        for (int i = 0; i < N_total && !nonNulle; i++) {
            for (int j = 0; j < N_total && !nonNulle; j++) {
                if (Math.abs(exactSol[i][j]) > 1e-12) { // Tolérance pour "non nul"
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
        if (valeurs == null || valeurs.length == 0) return Double.NaN;
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
     * @param casTest Cas de test
     * @param N_total Nombre total de points
     */
    public static void analyseSensibiliteConditions(Maillage.CasTest casTest, int N_total) {
        System.out.println("=== Analyse de sensibilité aux conditions aux limites (N_total=" + N_total + ") ===");
        SolveurGaussSeidel solveur = new SolveurGaussSeidel();
        double[] perturbations = {0.0, 0.1, 0.5, 1.0}; // Valeurs pour conditions constantes

        for (double perturbation : perturbations) {
            System.out.println("\nPerturbation (valeur CL constante): " + perturbation);
            ConditionsLimites conditions = new ConditionsLimites(perturbation, perturbation, perturbation, perturbation);
            Maillage maillage = new Maillage(N_total, conditions);
            maillage.configurerCasTest(casTest, conditions);

            SolveurGaussSeidel.ResultatConvergence resultat =
                solveur.resoudre(maillage, SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE);

            System.out.println("  Itérations: " + resultat.iterations);
            System.out.println("  Erreur finale (itérative): " + String.format("%.2e", resultat.erreurFinale));
            analyserNormeSolution(maillage);
        }
        solveur.fermer();
    }

    /**
     * Calcule différentes normes de la solution (sur les points intérieurs)
     */
    private static void analyserNormeSolution(Maillage maillage) {
        double[][] U = maillage.getU();
        int N_interieur = maillage.getN_interieur();
        double h = maillage.getH();

        if (N_interieur == 0) {
            System.out.println("  Normes solution: Pas de points intérieurs.");
            return;
        }

        double normeL2_carree = 0.0;
        double normeMax = 0.0;
        double normeL1 = 0.0;

        for (int i = 1; i <= N_interieur; i++) { // Indices globaux des points intérieurs
            for (int j = 1; j <= N_interieur; j++) {
                double valAbs = Math.abs(U[i][j]);
                normeL2_carree += U[i][j] * U[i][j]; // U_ij^2
                normeMax = Math.max(normeMax, valAbs);
                normeL1 += valAbs;
            }
        }
        // Approximations des intégrales:
        // ||u||_L1 = integral |u| dx dy approx h^2 * sum |u_ij|
        // ||u||_L2 = sqrt(integral u^2 dx dy) approx h * sqrt(sum u_ij^2)
        double valNormeL1 = h * h * normeL1;
        double valNormeL2 = h * Math.sqrt(normeL2_carree);


        System.out.println("  Normes solution (intérieure): L1=" + String.format("%.3e", valNormeL1) +
            ", L2=" + String.format("%.3e", valNormeL2) +
            ", L∞=" + String.format("%.3e", normeMax));
    }


    /**
     * Génère un rapport détaillé d'analyse avec conditions aux limites
     */
    public static String genererRapportComplet(ResultatAnalyse analyse, EtudeConvergence convergence,
                                               Maillage maillage) {
        StringBuilder rapport = new StringBuilder();
        rapport.append("=== RAPPORT D'ANALYSE AVEC CONDITIONS GÉNÉRALES ===\n\n");
        rapport.append("MAILLAGE:\n");
        rapport.append("   N_total: ").append(maillage.getN_total()).append("x").append(maillage.getN_total()).append("\n");
        rapport.append("   h: ").append(String.format("%.4f", maillage.getH())).append("\n\n");

        rapport.append("CONDITIONS AUX LIMITES:\n");
        rapport.append("   Type: ").append(maillage.getConditionsLimites().getDescription()).append("\n");
        rapport.append("   Compatibilité: ").append(maillage.getConditionsLimites().verifierCompatibilite() ? "OK" : "Problème").append("\n\n");

        rapport.append("ANALYSE DES ERREURS (par rapport à la solution exacte fournie):\n");
        rapport.append("   Type d'analyse: ").append(analyse.typeAnalyse).append("\n");
        rapport.append("   Solution exacte: ").append(analyse.solutionExacteDisponible ? "Disponible et valide" : "Non disponible/valide").append("\n");

        if (analyse.solutionExacteDisponible) {
            rapport.append("   Erreur L² (intégration numérique): ").append(String.format("%.6e", analyse.erreurL2)).append("\n");
            rapport.append("   Erreur maximum: ").append(String.format("%.6e", analyse.erreurMax)).append("\n");
            rapport.append("   Erreur moyenne: ").append(String.format("%.6e", analyse.erreurMoyenne)).append("\n");
        }
        rapport.append("   Points intérieurs analysés: ").append(analyse.pointsAnalyses).append("\n\n");

        if (convergence != null) {
            rapport.append("ORDRE DE CONVERGENCE:\n");
            rapport.append("   Ordre L² (moyen): ").append(String.format("%.2f", convergence.ordreConvergenceL2)).append("\n");
            rapport.append("   Ordre max (moyen): ").append(String.format("%.2f", convergence.ordreConvergenceMax)).append("\n");
            rapport.append("   Ordre théorique attendu: 2.0 (différences finies avec CL de Dirichlet)\n\n");
            rapport.append("DÉTAIL PAR MAILLAGE (N_total):\n");
            for (int i = 0; i < convergence.taillesN_total.length; i++) {
                rapport.append(String.format("   N_total=%-3d, h=%.4f, E_L2=%.2e, E_max=%.2e, E_disc_th=%.2e",
                    convergence.taillesN_total[i], convergence.pasMaillageH[i],
                    convergence.erreursL2[i], convergence.erreursMax[i],
                    convergence.erreursDiscretes[i]));
                if (i < convergence.ordresLocauxL2.length) {
                    rapport.append(String.format(", Ordre_L2_loc=%.2f, Ordre_max_loc=%.2f",
                        convergence.ordresLocauxL2[i], convergence.ordresLocauxMax[i]));
                }
                rapport.append("\n");
            }
        }

        rapport.append("\nVÉRIFICATIONS:\n");
        rapport.append("   Cohérence maillage (CL appliquées vs CL définies): ").append(maillage.verifierCoherence() ? "OK" : "Problème").append("\n");
        rapport.append("   Résidu RMS de l'équation (points intérieurs): ").append(String.format("%.2e", calculerResidu(maillage))).append("\n");
        return rapport.toString();
    }

    /**
     * Méthode utilitaire pour l'analyse rapide
     */
    public static void analyseRapide(Maillage maillage, SolveurGaussSeidel.ResultatConvergence resultat) {
        System.out.println("=== Analyse rapide ===");
        ResultatAnalyse analyse = calculerErreurs(maillage);
        System.out.println("Résolution: " + (resultat.converge ? "Convergée" : "Non convergée") +
            " en " + resultat.iterations + " itérations.");
        System.out.println("Résidu RMS (équation): " + String.format("%.2e", calculerResidu(maillage)));
        if (analyse.solutionExacteDisponible) {
            System.out.println("Erreur L² (discrétisation): " + String.format("%.6e", analyse.erreurL2));
            System.out.println("Erreur max (discrétisation): " + String.format("%.6e", analyse.erreurMax));
        }
        analyserDistributionSolution(maillage);
        System.out.println("======================");
    }
}
