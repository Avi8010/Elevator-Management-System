import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

enum dir {
    UP, DOWN, STOPPED
}

class Elv {
    private int elvId;
    private int currFlr;
    private dir dir;
    private List<Pair<Integer, Integer>> req;

    public Elv(int elvId, int initFlr) {
        this.elvId = elvId;
        this.currFlr = initFlr; // Initialize currFlr to 1 (flr one)
        this.dir = dir.STOPPED;
        this.req = new ArrayList<>();
    }

    public void setcurrFlr(int currFlr) {
        this.currFlr = currFlr;
    }

    public int get_elv_Id() {
        return elvId;
    }

    public int getcurrFlr() {
        return currFlr;
    }

    public dir getdir() {
        return dir;
    }

    /*public List<Integer> getreq() {
        return req;
    }*/
    public boolean isAtRequestedFloor(int flr) {
        return currFlr == flr && req.contains(flr);
    }


    public List<Pair<Integer, Integer>> getreq() {
        return req;
    }

    //  to add pairs (from_flr, to_flr) to the req list
    public void add_flr_req(int from_flr, int to_flr) {
        Pair<Integer, Integer> request = new Pair<>(from_flr, to_flr);
        if (!req.contains(request)) {
            if (dir == dir.STOPPED) {
                if (from_flr > currFlr) {
                    dir = dir.UP;
                } else if (from_flr < currFlr) {
                    dir = dir.DOWN;
                }
            }

            if (dir == dir.UP && from_flr > currFlr) {
                req.add(request);
            } else if (dir == dir.DOWN && from_flr < currFlr) {
                req.add(request);
            } else {
                int insertIndex = 0;
                for (int i = 0; i < req.size(); i++) {
                    Pair<Integer, Integer> floorPair = req.get(i);
                    int floor = floorPair.getLeft();
                    if ((dir == dir.UP && from_flr < floor) || (dir == dir.DOWN && from_flr > floor)) {
                        insertIndex = i;
                        break;
                    }
                }
                req.add(insertIndex, request);
            }
        }
    }

    public boolean hasPendingreqIndir(dir dir, int currFlr) {
        for (Pair<Integer, Integer> request : req) {
            int from_flr = request.getLeft();
            int to_flr = request.getRight();
            if ((from_flr > currFlr && dir == dir.UP) || (from_flr < currFlr && dir == dir.DOWN)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtRequestedFloor(int from_flr, int to_flr) {
        return currFlr == from_flr && req.contains(new Pair<>(from_flr, to_flr));
    }


    public void moveUp() {
        System.out.println("Elv " + elvId + " is moving UP from flr " + currFlr + ".");
        currFlr++;
        if (isAtRequestedFloor(currFlr, currFlr) || isAtRequestedFloor(currFlr, currFlr + 1)) {
            stop();
        }
    }

    public void moveDown() {
        System.out.println("Elv " + elvId + " is moving DOWN from flr " + currFlr + ".");
        currFlr--;
        if (isAtRequestedFloor(currFlr, currFlr) || isAtRequestedFloor(currFlr, currFlr - 1)) {
            stop();
        }
    }

    public void stop() {
        System.out.println("Elv " + elvId + " has STOPPED at flr " + currFlr + ".");
        if (isAtRequestedFloor(currFlr, currFlr) || isAtRequestedFloor(currFlr, currFlr + 1)) {
            req.remove(new Pair<>(currFlr, currFlr));
        }
        if (req.isEmpty()) {
            dir = dir.STOPPED;
        }
    }

}

class Pair<L, R> {
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    //  need to override equals and hashCode methods for Pair to work correctly in lists and maps
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}


class Elv_Ctrl {
    private List<Elv> elvs;
    private int num_flr;
    private Elv_Sim elv_sim;
    private boolean reqServed = true; // To keep track if req have been served

    public Elv_Ctrl(int num_flr) {
        elvs = new ArrayList<>();
        elvs.add(new Elv(1, 0)); // Elv 1 (top flr)
        elvs.add(new Elv(2, num_flr / 2)); // Elv 2 (middle flr)
        elvs.add(new Elv(3, num_flr-1)); // Elv 3 (bottom flr)

        this.num_flr = num_flr;
    }

    private int getInitialFloorForElevator(int elvId) {
        switch (elvId) {
            case 1:
                return 0; // Top flr
            case 2:
                return num_flr / 2; // Middle flr
            case 3:
                return num_flr-1; // Bottom flr
            default:
                return 0; // Default to bottom flr
        }
    }


    public void setElevatorCallback(Elv_Sim elv_sim) {
        this.elv_sim = elv_sim;
    }

    public List<Elv> getelvs() {
        return elvs;
    }

    private Elv find_close_elv(int from_flr) {
        int minDist = Integer.MAX_VALUE;
        Elv sel_elv = null;

        for (Elv elv : elvs) {
            if (elv.hasPendingreqIndir(dir.UP, elv.getcurrFlr()) ||
                    elv.hasPendingreqIndir(dir.DOWN, elv.getcurrFlr()) ||
                    elv.getdir() == dir.STOPPED) {
                int dist = Math.abs(elv.getcurrFlr() - from_flr);
                if (dist < minDist) {
                    minDist = dist;
                    sel_elv = elv;
                }
            }
        }
        return sel_elv;
    }

    private Elv find_idle_elv() {
        for (Elv elv : elvs) {
            if (elv.getdir() == dir.STOPPED) {
                return elv;
            }
        }
        return null;
    }

    public void req_elv(Pair<Integer, Integer> request) {
        int from_flr = request.getLeft();
        int to_flr = request.getRight();

        Elv sel_elv = find_close_elv(from_flr);
        if (sel_elv == null) {
            sel_elv = find_idle_elv();
        }

        if (sel_elv != null) {
            move_elv(sel_elv, new Pair<>(from_flr, to_flr)); // Pass the floor requests as separate parameters
        }
    }



    public void move_elv(Elv elv, Pair<Integer, Integer> request) {
        int fromFlr = request.getLeft();
        int toFlr = request.getRight();

        if (elv.getcurrFlr() == toFlr) {
            elv.stop();
            if (elv.isAtRequestedFloor(toFlr)) {
                elv_sim.update_flr_color(toFlr, Color.BLACK);
            }
        } else {
            if (elv.getcurrFlr() < toFlr) {
                elv.moveUp();
            } else {
                elv.moveDown();
            }

            elv_sim.update_elv_state(elv.get_elv_Id(), elv.getcurrFlr(), elv.getdir());

            if (elv.isAtRequestedFloor(toFlr)) {
                elv_sim.update_flr_color(toFlr, Color.RED);
            }
        }
    }





    private int calculateTotalEnergyConsumption() {
        int totalEnergyConsumption = 0;
        for (Elv elv : elvs) {
            int distanceToInitialPosition = Math.abs(elv.getcurrFlr() - getInitialFloorForElevator(elv.get_elv_Id()));
            totalEnergyConsumption += distanceToInitialPosition;
        }
        return totalEnergyConsumption;
    }


    public void swapElevatorInitialFloors() {
        int num_elvs = elvs.size();
        int idleAndNotAtInitialCount = 0;

        // Check how many elvs are both idle and not at their initial positions
        for (Elv elv : elvs) {
            int initFlr = getInitialFloorForElevator(elv.get_elv_Id());
            if (elv.getdir() == dir.STOPPED && elv.getcurrFlr() != initFlr) {
                idleAndNotAtInitialCount++;
            }
        }

        // Swap initial positions only if more than one elv is in the specified state
        if (idleAndNotAtInitialCount > 1) {
            int[] initFlrs = new int[num_elvs];

            // Store the current initial floors of elvs
            for (int i = 0; i < num_elvs; i++) {
                initFlrs[i] = getInitialFloorForElevator(elvs.get(i).get_elv_Id());
            }

            int initialEnergyConsumption = calculateTotalEnergyConsumption();
            // Find the best initial position for each elv to reduce energy consumption
            for (int i = 0; i < num_elvs; i++) {
                int originalInitialFloor = initFlrs[i];
                for (int j = 0; j < num_elvs; j++) {
                    if (i == j) continue;

                    // Temporarily swap initial positions
                    elvs.get(i).setcurrFlr(initFlrs[j]);
                    elvs.get(j).setcurrFlr(originalInitialFloor);

                    // Calculate the total energy consumption after the swap
                    int newEnergyConsumption = calculateTotalEnergyConsumption();

                    // If the new energy consumption is lower, keep the swap
                    if (newEnergyConsumption < initialEnergyConsumption) {
                        initialEnergyConsumption = newEnergyConsumption;
                    } else {
                        // Otherwise, revert the swap
                        elvs.get(i).setcurrFlr(originalInitialFloor);
                        elvs.get(j).setcurrFlr(initFlrs[j]);
                    }
                }
            }
        }
    }


    public void run_elvs() {
        int all_elvs_idle = 3;

        for (Elv elv : elvs) {
            if (!elv.getreq().isEmpty()) {
                all_elvs_idle--;
                Pair<Integer, Integer> request = elv.getreq().get(0); // Get the first request from the list
                int from_flr = request.getLeft();
                int to_flr = request.getRight();

                if (elv.getdir() == dir.UP) {
                    move_elv(elv, new Pair<>(from_flr, to_flr)); // Pass the floor requests as separate parameters
                } else if (elv.getdir() == dir.DOWN) {
                    move_elv(elv, new Pair<>(from_flr, to_flr)); // Pass the floor requests as separate parameters
                } else {
                    move_elv(elv, new Pair<>(from_flr, to_flr)); // Pass the floor requests as separate parameters
                }
            } else {
                if (all_elvs_idle >= 2) {
                    // We don't need to perform the swapping logic for this update
                    // If the elevator is idle and there are at least 2 idle elevators, move it back to its initial position
                    swapElevatorInitialFloors();
                    int initFlr = getInitialFloorForElevator(elv.get_elv_Id());
                    move_elv(elv,new Pair<>(elv.getcurrFlr(), initFlr)); // Pass the floor requests as separate parameters
                } else {
                    // If the elevator is idle and there's only one idle elevator, keep it idle
                    elv.stop();
                }
            }
        }
    }


}

public class Elv_Sim extends JPanel {
    private static final int NUM_FLR = 11;
    private static final int FLR_HEIGHT = 30;
    private static final int ELV_WIDTH = 50;
    private static final int ELV_HEIGHT = 30;

    private int flr_height;
    private int panelHeight;
    private int elv_Y_pos;

    private Map<Integer, JLabel> elv_labels;
    private Elv_Ctrl elv_ctrl;
    private Map<Integer, Color> flr_hlts_map = new HashMap<>();
    private boolean reqServed = true; // To keep track if req have been served

    public Elv_Ctrl getElv_Ctrl() {
        return elv_ctrl;
    }

    public Elv_Sim(int num_elvs, int num_flr) {
        flr_height = FLR_HEIGHT;
        panelHeight = (NUM_FLR + 1) * flr_height;
        elv_Y_pos = panelHeight - flr_height - ELV_HEIGHT;

        setPreferredSize(new Dimension(600, panelHeight));
        setBackground(Color.WHITE);

        elv_ctrl = new Elv_Ctrl(num_flr);
        elv_ctrl.setElevatorCallback(this);

        elv_labels = new HashMap<>();
        for (int i = 1; i <= num_elvs; i++) {
            JLabel label = new JLabel("Elv " + i + " - Floor 1");
            elv_labels.put(i, label);
            add(label);
        }

        int delay = 10000; // 500 milliseconds (0.5 seconds)
        ActionListener timerAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (reqServed) {
                    gen_rand_req();
                }
                elv_ctrl.run_elvs();
                update_flr_hlts();
                repaint();
            }
        };
        Timer timer = new Timer(delay, timerAction);
        timer.start();
    }

    private void update_flr_hlts() {
        for (int flr = 0; flr <= NUM_FLR; flr++) {
            Color hlts_color = flr_hlts_map.getOrDefault(flr, Color.BLACK);
            flr_hlts_map.put(flr, hlts_color);
        }

        for (Elv elv : elv_ctrl.getelvs()) {
            if (elv.isAtRequestedFloor(elv.getcurrFlr())) {
                flr_hlts_map.put(elv.getcurrFlr(), Color.BLACK);
            }
        }
    }


    private void gen_rand_req() {
        Random random = new Random();
        int num_flr = NUM_FLR - 1;
        int from_flr = random.nextInt(num_flr)+1; // Generate random flr between 1 and 10
        int to_flr = random.nextInt(num_flr) + 1; // Generate random flr between 1 and 10

        while (from_flr == to_flr) {
            to_flr = random.nextInt(num_flr) + 1; // Ensure "to_flr" is different from "from_flr"
        }

        // Check if the generated flr values are within valid range (1 to 10)
        if (from_flr >= 1 && from_flr < 10 && to_flr >= 0 && to_flr <= 10) {
            // Clear the flr_hlts_map to reset previous req
            flr_hlts_map.clear();

            // Add the new request to the flr_hlts_map
            flr_hlts_map.put(from_flr, Color.RED);

            elv_ctrl.req_elv(new Pair<>(from_flr, to_flr));
            repaint();

        }
    }


    public void update_elv_state(int elvId, int currFlr, dir dir) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = elv_labels.get(elvId);
            label.setText("Elv " + elvId + " - Floor " + currFlr);
            repaint();
        });
    }

    public void update_flr_color(int flr, Color color) {
        SwingUtilities.invokeLater(() -> {
            flr_hlts_map.put(flr, color);
            repaint();
        });
    }



    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int flr = NUM_FLR - 1; flr >= 0; --flr) {
            Color hlts_color = flr_hlts_map.getOrDefault(flr, Color.BLACK);
            g.setColor(hlts_color);
            g.drawString("Floor " + flr, 10, (NUM_FLR - flr) * flr_height + 20);
        }

        int num_elvs = elv_labels.size();
        int elv_spacing = (getWidth() - ELV_WIDTH * num_elvs) / (num_elvs + 1);
        for (int elvId = 1; elvId <= num_elvs; elvId++) {
            g.setColor(Color.BLUE);
            Elv elv = elv_ctrl.getelvs().get(elvId - 1);
            int x = elv_spacing * elvId + ELV_WIDTH * (elvId - 1);
            int y = elv_Y_pos - (elv.getcurrFlr() - 1) * flr_height;
            int targetY = y;

            // Add a smooth animation effect
            if (elv.getdir() == dir.UP) {
                targetY = elv_Y_pos - (elv.getcurrFlr()) * flr_height;
            } else if (elv.getdir() == dir.DOWN) {
                targetY = elv_Y_pos - (elv.getcurrFlr() - 2) * flr_height;
            }

            int deltaY = (targetY - y) / 10; // Number of animation steps

            // Drawing the elv at the updated position
            g.fillRect(x, y + deltaY, ELV_WIDTH, ELV_HEIGHT);
        }
    }

    public static void main(String[] args) {
        int num_elvs = 3;
        int num_flr = Elv_Sim.NUM_FLR;

        Elv_Sim elv_sim = new Elv_Sim(num_elvs, num_flr);
        elv_sim.setPreferredSize(new Dimension(600, elv_sim.panelHeight));
        JFrame frame = new JFrame("Elevator Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(elv_sim);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Elv_Ctrl elv_ctrl = elv_sim.getElv_Ctrl();

        // Hardcoded test case
        int t1=1,t2=3,t3=9;
        int f1=9,f2=7,f3=1;
        //while(true) {

        elv_ctrl.getelvs().get(2).add_flr_req((t1+++1)%10, (f1+++1)%10);
        elv_ctrl.getelvs().get(0).add_flr_req((t2+++1)%10, (f2+++1)%10);
        elv_ctrl.getelvs().get(1).add_flr_req((t3+++1)%10, (f3+++1)%10);
            /*elv_ctrl.getelvs().get(2).add_flr_req(9, 2);

            elv_ctrl.getelvs().get(1).add_flr_req(3, 7);
            elv_ctrl.getelvs().get(0).add_flr_req(6, 9);*/

        int delay = 1000; // 1000 milliseconds (1 second)
        ActionListener timerAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                elv_ctrl.run_elvs();
                elv_sim.update_flr_hlts();

                elv_sim.repaint();
            }
        };
        Timer timer = new Timer(delay, timerAction);
        timer.start();

    }
}