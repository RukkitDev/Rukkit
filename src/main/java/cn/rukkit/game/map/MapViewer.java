package cn.rukkit.game.map;

import javax.swing.*;

import cn.rukkit.game.map.MapParser.MapInfo;
import cn.rukkit.game.unit.Unit;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class MapViewer extends JFrame {
    private MapInfo mapInfo;
    private MapPanel mapPanel;
    private JScrollPane scrollPane;
    
    // 颜色映射 - 为不同的图块ID分配不同的颜色
    private Map<Integer, Color> colorMap = new HashMap<>();
    
    public MapViewer(MapInfo mapInfo) {
        this.mapInfo = mapInfo;
        initColorMap();
        initUI();
    }
    
    private void initColorMap() {
        // 为不同的图块ID预定义一些颜色
        colorMap.put(0, Color.BLACK); // 空图块
        colorMap.put(1, Color.GREEN);
        colorMap.put(2, Color.BLUE);
        colorMap.put(3, Color.RED);
        colorMap.put(4, Color.YELLOW);
        colorMap.put(5, Color.CYAN);
        colorMap.put(6, Color.MAGENTA);
        colorMap.put(7, Color.ORANGE);
        colorMap.put(8, Color.PINK);
        colorMap.put(9, Color.GRAY);
        colorMap.put(10, Color.LIGHT_GRAY);
        // 可以继续添加更多颜色映射...
    }
    
    private void initUI() {
        setTitle("TMX Map Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        mapPanel = new MapPanel();
        scrollPane = new JScrollPane(mapPanel);
        add(scrollPane);
        
        // 添加窗口监听器，在窗口显示时自动调整滚动条
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                mapPanel.adjustScrollBars();
            }
        });
    }
    
    // 根据图块ID获取颜色
    private Color getColorForTileId(int tileId) {
        // 如果已经有预定义的颜色，使用预定义颜色
        if (colorMap.containsKey(tileId)) {
            return colorMap.get(tileId);
        }
        
        // 否则使用哈希算法生成一个稳定的颜色
        int hash = Integer.hashCode(tileId);
        float hue = (hash & 0xFF) / 255.0f;
        float saturation = 0.7f + ((hash >> 8) & 0xFF) / 255.0f * 0.3f;
        float brightness = 0.6f + ((hash >> 16) & 0xFF) / 255.0f * 0.4f;
        
        return Color.getHSBColor(hue, saturation, brightness);
    }
    
    // 地图绘制面板
    class MapPanel extends JPanel {
        private static final int TILE_SIZE = 16; // 每个图块显示的大小
        
        public MapPanel() {
            setPreferredSize(new Dimension(
                mapInfo.width * TILE_SIZE, 
                mapInfo.height * TILE_SIZE
            ));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 按照层级顺序绘制：Ground -> Items -> Units
            drawLayer(g2d, mapInfo.groundTiles, 0.7f);    // Ground层半透明
            drawLayer(g2d, mapInfo.items, 0.8f);         // Items层
            drawLayer(g2d, mapInfo.units, 1.0f);         // Units层不透明
            
            // 绘制网格线（可选）
            drawGrid(g2d);
        }
        
        private void drawLayer(Graphics2D g2d, java.util.List<?> layer, float alpha) {
            if (layer == null || layer.isEmpty()) return;
            
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            if (layer.get(0) instanceof MapParser.MapTile) {
                // 绘制地面层
                for (MapParser.MapTile tile : (java.util.List<MapParser.MapTile>) layer) {
                    Color color = getColorForTileId(tile.tileId);
                    g2d.setColor(color);
                    g2d.fillRect(tile.x * TILE_SIZE, tile.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    
                    // 绘制图块ID（可选）
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    g2d.drawString(String.valueOf(tile.tileId), 
                                  tile.x * TILE_SIZE + 2, 
                                  tile.y * TILE_SIZE + 10);
                }
            } else if (layer.get(0) instanceof MapParser.MapItem) {
                // 绘制物品层
                for (MapParser.MapItem item : (java.util.List<MapParser.MapItem>) layer) {
                    Color color = getColorForTileId(item.itemId);
                    g2d.setColor(color);
                    // 物品用圆形表示
                    g2d.fillOval(item.x * TILE_SIZE, item.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    g2d.drawString(String.valueOf(item.itemId), 
                                  item.x * TILE_SIZE + 4, 
                                  item.y * TILE_SIZE + 10);
                }
            } else if (layer.get(0) instanceof Unit) {
                // 绘制单位层
                for (Unit unit : (java.util.List<Unit>) layer) {
                    int x = (int) (unit.pixelX / mapInfo.tileWidth);
                    int y = (int) (unit.pixelY / mapInfo.tileHeight);
                    
                    Color color = getColorForTileId(unit.unitId);
                    g2d.setColor(color);
                    // 单位用菱形表示
                    int[] xPoints = {
                        x * TILE_SIZE + TILE_SIZE / 2,
                        x * TILE_SIZE + TILE_SIZE,
                        x * TILE_SIZE + TILE_SIZE / 2,
                        x * TILE_SIZE
                    };
                    int[] yPoints = {
                        y * TILE_SIZE,
                        y * TILE_SIZE + TILE_SIZE / 2,
                        y * TILE_SIZE + TILE_SIZE,
                        y * TILE_SIZE + TILE_SIZE / 2
                    };
                    g2d.fillPolygon(xPoints, yPoints, 4);
                    
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    g2d.drawString(String.valueOf(unit.unitId), 
                                  x * TILE_SIZE + 4, 
                                  y * TILE_SIZE + 10);
                }
            }
            
            g2d.setComposite(originalComposite);
        }
        
        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 50)); // 半透明黑色
            g2d.setStroke(new BasicStroke(0.5f));
            
            // 绘制垂直线
            for (int x = 0; x <= mapInfo.width; x++) {
                g2d.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, mapInfo.height * TILE_SIZE);
            }
            
            // 绘制水平线
            for (int y = 0; y <= mapInfo.height; y++) {
                g2d.drawLine(0, y * TILE_SIZE, mapInfo.width * TILE_SIZE, y * TILE_SIZE);
            }
        }
        
        public void adjustScrollBars() {
            // 自动调整滚动条到地图中心
            JViewport viewport = scrollPane.getViewport();
            int centerX = (getWidth() - viewport.getWidth()) / 2;
            int centerY = (getHeight() - viewport.getHeight()) / 2;
            viewport.setViewPosition(new Point(centerX, centerY));
        }
    }
    
    // 主方法
    public static void main(String[] args) {
        // if (args.length == 0) {
        //     System.out.println("Usage: java MapViewer <tmx-file-path>");
        //     return;
        // }
        
        String filePath = "/media/micro/Work_Space/Rukkit-master/data/maps/skirmish/[p8]Two Sides (8p).tmx";
        
        // 在事件调度线程中运行GUI
        SwingUtilities.invokeLater(() -> {
            try {
                // 解析地图
                MapParser parser = new MapParser(filePath);
                MapInfo mapInfo = parser.getMapInfo();
                
                if (mapInfo == null) {
                    JOptionPane.showMessageDialog(null, "Failed to parse map file: " + filePath);
                    return;
                }
                
                // 创建并显示地图查看器
                MapViewer viewer = new MapViewer(mapInfo);
                viewer.setVisible(true);
                
                // 打印解析信息
                System.out.println("Map loaded successfully:");
                System.out.println("Size: " + mapInfo.width + "x" + mapInfo.height);
                System.out.println("Tile size: " + mapInfo.tileWidth + "x" + mapInfo.tileHeight);
                System.out.println("Ground tiles: " + mapInfo.groundTiles.size());
                System.out.println("Items: " + mapInfo.items.size());
                System.out.println("Units: " + mapInfo.units.size());
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        });
    }
}