/**
 * Point d'entrée principal du programme de résolution par différences finies 2D
 *
 * Ce programme résout l'équation -ΔU = f sur un domaine 2D carré [0,1]×[0,1]
 * avec conditions aux limites de Dirichlet générales U(0)=U₀, U(1)=U₁.
 *
 * Méthodes implémentées :
 * - Gauss-Seidel classique
 * - Gauss-Seidel avec relaxation (SOR)
 * - Gauss-Seidel parallélisé
 *
 * Fonctionnalités :
 * - Calcul de l'erreur en norme L² par intégration numérique
 * - Calcul de l'ordre numérique de convergence
 * - Représentation graphique avec système de coloration
 * - Représentation graphique avec courbes de niveau (contours)
 * - Configuration flexible des conditions aux limites
 */
public class Main {

    /**
     * Point d'entrée principal du programme
     * Initialise l'interface graphique principale
     *
     * @param args Arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        // Configuration du Look and Feel système pour une meilleure intégration
        try {
            javax.swing.UIManager.setLookAndFeel(
                javax.swing.UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            System.err.println("Impossible de charger le Look and Feel système: " + e.getMessage());
        }

        // Lancement de l'interface graphique dans le thread EDT (Event Dispatch Thread)
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Création et affichage de la fenêtre principale
                DifferencesFinis2DInterface mainInterface = new DifferencesFinis2DInterface();
                mainInterface.setVisible(true);

                System.out.println("=== Solveur Différences Finies 2D ===");
                System.out.println("Interface graphique lancée avec succès.");
                System.out.println("Équation résolue : -ΔU = f");
                System.out.println("Domaine : [0,1] × [0,1]");
                System.out.println("Conditions aux limites : U(bord) = valeurs spécifiées");
                System.out.println("Discrétisation : Schéma 5 points");
                System.out.println("=====================================");

            } catch (Exception e) {
                System.err.println("Erreur lors du lancement de l'interface : " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });

        // Ajout d'un hook pour nettoyer les ressources à la fermeture
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Fermeture du programme...");
            // Ici on pourrait ajouter du nettoyage de ressources si nécessaire
        }));
    }

    /**
     * Affiche les informations sur l'utilisation du programme
     */
    public static void printUsageInfo() {
        System.out.println("Usage: java Main");
        System.out.println();
        System.out.println("Ce programme lance une interface graphique pour résoudre");
        System.out.println("l'équation de Poisson -ΔU = f par différences finies.");
        System.out.println();
        System.out.println("Fonctionnalités disponibles :");
        System.out.println("- Choix de différents cas de test");
        System.out.println("- Configuration des conditions aux limites");
        System.out.println("- Sélection de la méthode de résolution");
        System.out.println("- Visualisation avec coloration");
        System.out.println("- Visualisation avec courbes de niveau");
        System.out.println("- Analyse de convergence");
        System.out.println("- Calcul d'erreurs");
    }
}
