import {DataGrid, GridColDef} from "@mui/x-data-grid";
import {DateField, List, useDataGrid, ShowButton} from "@refinedev/mui";
import React from "react";

export const ReceivedEventList = () => {

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
          minWidth: 140,
          renderCell: (params: any) => <DateField format="YYYY-MM-DD HH:mm" value={params.value}/>
        },
        {
          field: "messageType",
          headerName: "Message Type",
          type: "string",
          minWidth: 100,
        },
        {
          field: "origin",
          headerName: "Origin",
          type: "string",
          minWidth: 300,
        },
        {
          field: "Status",
          headerName: "Status",
          type: "string",
          minWidth: 80,
        },
        {
          field: "actions",
          headerName: "Actions",
          renderCell: function render({row}) {
            return
                <ShowButton size="small" recordItemId={row.id} />;
          },
          align: "center",
          headerAlign: "center",
          minWidth: 10,
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