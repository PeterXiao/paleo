package ch.fhnw.ima.paleo;

public interface Column<T extends ColumnId> {

    T getColumnId();

    int getRowCount();

}