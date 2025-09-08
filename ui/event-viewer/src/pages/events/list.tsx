import {DataGrid, GridColDef} from "@mui/x-data-grid";
import {DateField, List, useDataGrid, } from "@refinedev/mui";
import React from "react";

export const EventList = () => {
  const {dataGridProps} = useDataGrid({
    syncWithLocation: true,
  });

  const columns = React.useMemo<GridColDef[]>(
    () => {
      return [
        {
          field: "messageId",
          headerName: "messageID",
          type: "string",
          minWidth: 150,
        },
        {
          field: "date",
          headerName: "Date",
          type: "string",
          minWidth: 150,
          renderCell: (params: any) => <DateField format="YYYY-MM-DD HH:mm" value={params.value}/>
        },
        {
          field: "messageType",
          headerName: "Type",
          type: "string",
          minWidth: 100,
        },
        {
          field: "Status",
          headerName: "Status",
          type: "string",
          minWidth: 80,
        }
      ];
    },
    []
  );

  return (
    <List>
      <DataGrid {...dataGridProps} columns={columns} getRowId={(row: any) =>  row.messageId} autoHeight/>
    </List>
  );

};
