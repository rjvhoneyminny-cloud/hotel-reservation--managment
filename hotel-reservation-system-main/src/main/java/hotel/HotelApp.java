package hotel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class HotelApp extends JFrame {
    private final DefaultTableModel roomsModel = model("ID", "Room", "Type", "Capacity", "Rate", "Active");
    private final DefaultTableModel guestsModel = model("ID", "Name", "Email", "Phone");
    private final DefaultTableModel reservationsModel = model("ID", "Room", "Guest", "Check in", "Check out", "Status");
    private final JTable roomsTable = new JTable(roomsModel), guestsTable = new JTable(guestsModel), reservationsTable = new JTable(reservationsModel);
    private final JComboBox<Room> roomChoice = new JComboBox<>();
    private final JComboBox<Guest> guestChoice = new JComboBox<>();
    private final JTextField inField = new JTextField(LocalDate.now().plusDays(1).toString(), 10), outField = new JTextField(LocalDate.now().plusDays(2).toString(), 10);
    private final Map<Integer, Room> roomCache = new HashMap<>();
    private final java.util.List<Guest> guestCache = new ArrayList<>();
    private final java.util.List<Reservation> reservationCache = new LinkedList<>();

    public HotelApp() {
        super("Hotel Room Reservation System"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(980, 620); setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane(); tabs.add("Reservations", reservationsPanel()); tabs.add("Rooms", roomsPanel()); tabs.add("Guests", guestsPanel()); add(tabs);
        JMenuBar bar = new JMenuBar(); JMenu file = new JMenu("File");
        JMenuItem csv = new JMenuItem("Export reservations CSV"), backup = new JMenuItem("Save serialized backup"), restore = new JMenuItem("Restore backup");
        csv.addActionListener(e -> exportCsv()); backup.addActionListener(e -> backup()); restore.addActionListener(e -> restore());
        file.add(csv); file.add(backup); file.add(restore); bar.add(file); setJMenuBar(bar);
        refreshAll();
    }
    private static DefaultTableModel model(String... cols) { return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} }; }
    private JPanel reservationsPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8)); JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Room")); form.add(roomChoice); form.add(new JLabel("Guest")); form.add(guestChoice);
        form.add(new JLabel("Check-in (YYYY-MM-DD)")); form.add(inField); form.add(new JLabel("Check-out")); form.add(outField);
        JButton add = new JButton("Book"), cancel = new JButton("Cancel selected"), refresh = new JButton("Refresh");
        add.addActionListener(e -> createReservation()); cancel.addActionListener(e -> cancelReservation()); refresh.addActionListener(e -> refreshAll());
        form.add(add); form.add(cancel); form.add(refresh); p.add(form, BorderLayout.NORTH); p.add(new JScrollPane(reservationsTable));
        return p;
    }
    private JPanel roomsPanel() {
        JPanel p=new JPanel(new BorderLayout()); JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton create=new JButton("Add room"), update=new JButton("Update selected"), delete=new JButton("Delete selected");
        create.addActionListener(e->roomDialog(null)); update.addActionListener(e->roomDialog(selectedId(roomsTable))); delete.addActionListener(e->deleteRoom());
        actions.add(create);actions.add(update);actions.add(delete);p.add(actions,BorderLayout.NORTH);p.add(new JScrollPane(roomsTable));return p;
    }
    private JPanel guestsPanel() {
        JPanel p=new JPanel(new BorderLayout()); JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton create=new JButton("Add guest"), update=new JButton("Update selected"), delete=new JButton("Delete selected");
        create.addActionListener(e->guestDialog(null)); update.addActionListener(e->guestDialog(selectedId(guestsTable))); delete.addActionListener(e->deleteGuest());
        actions.add(create);actions.add(update);actions.add(delete);p.add(actions,BorderLayout.NORTH);p.add(new JScrollPane(guestsTable));return p;
    }
    private Integer selectedId(JTable t) { int row=t.getSelectedRow(); if(row<0){error("Select a row first.");return null;}return (Integer)t.getValueAt(row,0); }
    private void roomDialog(Integer id) {
        JTextField n=new JTextField(), type=new JTextField(), cap=new JTextField(), rate=new JTextField(); JCheckBox active=new JCheckBox("Active",true);
        if(id!=null){try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("SELECT * FROM rooms WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next()){n.setText(r.getString("room_number"));type.setText(r.getString("room_type"));cap.setText(""+r.getInt("capacity"));rate.setText(r.getBigDecimal("nightly_rate").toPlainString());active.setSelected(r.getBoolean("active"));}}}catch(SQLException ex){showError(ex);return;}}
        Object[] fields={"Room number",n,"Type",type,"Capacity",cap,"Nightly rate",rate,active};
        if(JOptionPane.showConfirmDialog(this,fields,id==null?"Add room":"Update room",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{String number=n.getText().trim(), kind=type.getText().trim();int capacity=Integer.parseInt(cap.getText().trim());java.math.BigDecimal price=new java.math.BigDecimal(rate.getText().trim());if(number.isBlank()||kind.isBlank()||capacity<1||price.signum()<0)throw new IllegalArgumentException("Enter a room number, type, positive capacity and non-negative rate.");
            try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement(id==null?"INSERT INTO rooms(room_number,room_type,capacity,nightly_rate,active) VALUES(?,?,?,?,?)":"UPDATE rooms SET room_number=?,room_type=?,capacity=?,nightly_rate=?,active=? WHERE id=?")){s.setString(1,number);s.setString(2,kind);s.setInt(3,capacity);s.setBigDecimal(4,price);s.setBoolean(5,active.isSelected());if(id!=null)s.setInt(6,id);s.executeUpdate();}refreshAll();
        }catch(Exception ex){showError(ex);}
    }
    private void guestDialog(Integer id) {
        JTextField name=new JTextField(),email=new JTextField(),phone=new JTextField();
        if(id!=null){try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("SELECT * FROM guests WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next()){name.setText(r.getString("full_name"));email.setText(r.getString("email"));phone.setText(r.getString("phone"));}}}catch(SQLException ex){showError(ex);return;}}
        Object[] fields={"Full name",name,"Email",email,"Phone",phone};if(JOptionPane.showConfirmDialog(this,fields,id==null?"Add guest":"Update guest",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{String nm=name.getText().trim(),em=email.getText().trim(),ph=phone.getText().trim();if(nm.isBlank()||!em.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||ph.isBlank())throw new IllegalArgumentException("Enter a name, valid email and phone number.");
            try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement(id==null?"INSERT INTO guests(full_name,email,phone) VALUES(?,?,?)":"UPDATE guests SET full_name=?,email=?,phone=? WHERE id=?")){s.setString(1,nm);s.setString(2,em);s.setString(3,ph);if(id!=null)s.setInt(4,id);s.executeUpdate();}refreshAll();
        }catch(Exception ex){showError(ex);}
    }
    private void deleteRoom(){Integer id=selectedId(roomsTable);if(id==null)return;if(confirmDelete())try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("DELETE FROM rooms WHERE id=?")){s.setInt(1,id);s.executeUpdate();refreshAll();}catch(SQLException ex){showError(ex);}}
    private void deleteGuest(){Integer id=selectedId(guestsTable);if(id==null)return;if(confirmDelete())try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("DELETE FROM guests WHERE id=?")){s.setInt(1,id);s.executeUpdate();refreshAll();}catch(SQLException ex){showError(ex);}}
    private boolean confirmDelete(){return JOptionPane.showConfirmDialog(this,"Delete selected record? Existing reservations may prevent deletion.","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private void createReservation(){try{Room room=(Room)roomChoice.getSelectedItem();Guest guest=(Guest)guestChoice.getSelectedItem();if(room==null||guest==null)throw new IllegalArgumentException("Add at least one active room and guest first.");LocalDate from=LocalDate.parse(inField.getText().trim()),to=LocalDate.parse(outField.getText().trim());if(!to.isAfter(from))throw new IllegalArgumentException("Check-out must be after check-in.");
        try(Connection c=Db.connect();PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM reservations WHERE room_id=? AND status='BOOKED' AND check_in < ? AND check_out > ?")){q.setInt(1,room.id());q.setDate(2,java.sql.Date.valueOf(to));q.setDate(3,java.sql.Date.valueOf(from));try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)>0)throw new IllegalArgumentException("That room is already booked for overlapping dates.");}}
        try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("INSERT INTO reservations(room_id,guest_id,check_in,check_out) VALUES(?,?,?,?)")){s.setInt(1,room.id());s.setInt(2,guest.id());s.setDate(3,java.sql.Date.valueOf(from));s.setDate(4,java.sql.Date.valueOf(to));s.executeUpdate();}refreshAll();JOptionPane.showMessageDialog(this,"Reservation created.");
    }catch(Exception ex){showError(ex);}}
    private void cancelReservation(){Integer id=selectedId(reservationsTable);if(id==null)return;try(Connection c=Db.connect();PreparedStatement s=c.prepareStatement("UPDATE reservations SET status='CANCELLED' WHERE id=?")){s.setInt(1,id);s.executeUpdate();refreshAll();}catch(SQLException ex){showError(ex);}}
    private void refreshAll(){try{roomsModel.setRowCount(0);guestsModel.setRowCount(0);reservationsModel.setRowCount(0);roomCache.clear();guestCache.clear();reservationCache.clear();roomChoice.removeAllItems();guestChoice.removeAllItems();
        try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM rooms ORDER BY room_number")){while(r.next()){Room x=new Room(r.getInt("id"),r.getString("room_number"),r.getString("room_type"),r.getInt("capacity"),r.getBigDecimal("nightly_rate"),r.getBoolean("active"));roomCache.put(x.id(),x);roomsModel.addRow(new Object[]{x.id(),x.number(),x.type(),x.capacity(),x.rate(),x.active()});if(x.active())roomChoice.addItem(x);}}
        try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM guests ORDER BY full_name")){while(r.next()){Guest x=new Guest(r.getInt("id"),r.getString("full_name"),r.getString("email"),r.getString("phone"));guestCache.add(x);guestsModel.addRow(new Object[]{x.id(),x.name(),x.email(),x.phone()});guestChoice.addItem(x);}}
        try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT v.id,rm.room_number,g.full_name,v.check_in,v.check_out,v.status FROM reservations v JOIN rooms rm ON rm.id=v.room_id JOIN guests g ON g.id=v.guest_id ORDER BY v.check_in DESC")){while(r.next()){Reservation x=new Reservation(r.getInt("id"),r.getString("room_number"),r.getString("full_name"),r.getDate("check_in").toLocalDate(),r.getDate("check_out").toLocalDate(),r.getString("status"));reservationCache.add(x);reservationsModel.addRow(new Object[]{x.id(),x.room(),x.guest(),x.checkIn(),x.checkOut(),x.status()});}}
    }catch(SQLException ex){showError(ex);}}
    private void exportCsv(){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("reservations.csv"));if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try(Writer w=Files.newBufferedWriter(fc.getSelectedFile().toPath())){w.write("id,room,guest,check_in,check_out,status\n");for(Reservation r:reservationCache)w.write(r.id()+","+csv(r.room())+","+csv(r.guest())+","+r.checkIn()+","+r.checkOut()+","+r.status()+"\n");JOptionPane.showMessageDialog(this,"CSV exported.");}catch(IOException ex){showError(ex);}}
    private String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private void backup(){Path path=Path.of("hotel-backup.ser");try(ObjectOutputStream o=new ObjectOutputStream(Files.newOutputStream(path))){o.writeObject(new ArrayList<>(reservationCache));JOptionPane.showMessageDialog(this,"Serialized reservation backup saved to "+path.toAbsolutePath());}catch(IOException ex){showError(ex);}}
    @SuppressWarnings("unchecked") private void restore(){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("hotel-backup.ser"));if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(fc.getSelectedFile().toPath()))){Object obj=in.readObject();if(!(obj instanceof ArrayList<?> list)||(!list.isEmpty()&&!(list.get(0) instanceof Reservation)))throw new IOException("File is not a reservation backup.");reservationCache.clear();reservationCache.addAll((ArrayList<Reservation>)list);JOptionPane.showMessageDialog(this,"Backup loaded in memory for review. Database was not changed.");}catch(IOException|ClassNotFoundException ex){showError(ex);}}
    private void showError(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage()==null?ex.toString():ex.getMessage(),"Operation failed",JOptionPane.ERROR_MESSAGE);}
    private void error(String m){JOptionPane.showMessageDialog(this,m,"Notice",JOptionPane.WARNING_MESSAGE);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{try{Db.initialize();new HotelApp().setVisible(true);}catch(SQLException e){JOptionPane.showMessageDialog(null,"Cannot connect to MySQL. Check schema and DB settings.\n"+e.getMessage(),"Database error",JOptionPane.ERROR_MESSAGE);}});}
}
