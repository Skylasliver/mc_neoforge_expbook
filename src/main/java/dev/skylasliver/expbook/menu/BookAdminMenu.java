package dev.skylasliver.expbook.menu;
import dev.skylasliver.expbook.registry.ModMenus;
import dev.skylasliver.expbook.util.BookLedger;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import java.util.*;
public final class BookAdminMenu extends AbstractContainerMenu {
 public static final int PAGE_SIZE = 54, PREVIOUS = 54, NEXT = 55, TAKE_BASE = 56;
 private final UUID owner; private final List<UUID> ids; private final SimpleContainer display = new SimpleContainer(PAGE_SIZE);
 private final DataSlot page = DataSlot.standalone();
 public BookAdminMenu(int id,Inventory inv,RegistryFriendlyByteBuf b){this(id,inv,b.readUUID(),readIds(b));}
 private static List<UUID> readIds(RegistryFriendlyByteBuf b){int n=b.readVarInt(); List<UUID> r=new ArrayList<>(); for(int i=0;i<n;i++)r.add(b.readUUID()); return r;}
 public BookAdminMenu(int id,Inventory inv,UUID owner,List<UUID> ids){super(ModMenus.BOOK_ADMIN.get(),id);this.owner=owner;this.ids=List.copyOf(ids); addDataSlot(page); if(inv.player.level() instanceof net.minecraft.server.level.ServerLevel sl) refresh(sl); for(int i=0;i<PAGE_SIZE;i++) addSlot(new Slot(display,i,8+(i%9)*18,40+(i/9)*18){public boolean mayPickup(Player p){return false;} public boolean mayPlace(ItemStack s){return false;}});}
 public UUID owner(){return owner;} public List<UUID> ids(){return ids;}
 public int page(){return page.get();}
 public int pageCount(){return Math.max(1,(ids.size()+PAGE_SIZE-1)/PAGE_SIZE);}
 public UUID bookId(int slot){int index=page()*PAGE_SIZE+slot; return slot>=0&&slot<PAGE_SIZE&&index<ids.size()?ids.get(index):null;}
 private void refresh(net.minecraft.server.level.ServerLevel level){for(int i=0;i<PAGE_SIZE;i++){UUID id=bookId(i); display.setItem(i,id!=null&&BookLedger.isActive(level,id)?BookLedger.snapshot(level,id):ItemStack.EMPTY);}}
 @Override public boolean clickMenuButton(Player p,int b){
  if(!(p instanceof net.minecraft.server.level.ServerPlayer sp)||!stillValid(p))return false;
  if(b==PREVIOUS||b==NEXT){int next=page()+(b==PREVIOUS?-1:1); if(next<0||next>=pageCount())return false; page.set(next); refresh(sp.serverLevel()); broadcastChanges(); return true;}
  boolean take = b >= TAKE_BASE;
  if(take)b-=TAKE_BASE;
  if(b<0||b>=PAGE_SIZE||display.getItem(b).isEmpty())return false;
  UUID id=bookId(b); BookLedger.Entry entry=id==null?null:BookLedger.find(sp.serverLevel(),id);
  if(entry==null||!entry.owner().equals(owner))return false;
  boolean ok=!BookLedger.recover(sp,id,take?BookLedger.Delivery.ADMIN:BookLedger.Delivery.OWNER).isEmpty(); refresh(sp.serverLevel()); broadcastChanges(); return ok;
 }
 // Ledger snapshots must never be moved, swapped or cloned as inventory items.
 @Override public void clicked(int slot,int button,ClickType type,Player player){}
 @Override public boolean stillValid(Player p){return p.isAlive()&&p.hasPermissions(2);}
 @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
}
